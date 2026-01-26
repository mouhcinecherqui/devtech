import { Component, OnInit, signal, computed, inject, OnDestroy } from '@angular/core';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule, FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { Router, ActivatedRoute } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';
import { Subject, takeUntil } from 'rxjs';
import SharedModule from '../shared/shared.module';
import { TicketPaymentComponent } from '../ticket-payment/ticket-payment.component';
import { AppParametersService } from 'app/core/services/app-parameters.service';
import { AppParameter } from 'app/admin/parameters/parameters.model';
import { ClientReviewService } from 'app/core/services/client-review.service';
import { ClientReviewComponent } from 'app/client-review/client-review.component';

interface Ticket {
  id?: number;
  type: string;
  description: string;
  backofficeUrl?: string;
  backofficeLogin?: string;
  backofficePassword?: string;
  hostingUrl?: string;
  createdDate?: string;
  status?: string;
  imageUrl?: string;
  messages?: string[];
  messageStrings?: string[];
}

@Component({
  selector: 'jhi-ticket-detail',
  templateUrl: './ticket-detail.component.html',
  styleUrls: ['./ticket-detail.component.scss'],
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, FormsModule, SharedModule, TicketPaymentComponent, ClientReviewComponent],
})
export class TicketDetailComponent implements OnInit, OnDestroy {
  private readonly destroy$ = new Subject<void>();
  private readonly appParametersService = inject(AppParametersService);
  private readonly clientReviewService = inject(ClientReviewService);

  ticket = signal<Ticket | null>(null);
  loading = signal(false);
  error = signal<string | null>(null);
  success = signal<string | null>(null);
  workflowSteps = ['Nouveau', 'En cours', 'Résolu', 'En attente de paiement', 'Paiement validé', 'Fermé'];

  showReviewButton = signal<boolean>(false);
  hasReview = signal<boolean>(false);
  showReviewModal = signal<boolean>(false);

  // Client reply - utiliser une propriété normale au lieu d'un signal
  clientReply = '';
  sendingReply = signal<boolean>(false);

  // Image modal signals
  showImageModal = signal<boolean>(false);
  selectedImageUrl = signal<string | null>(null);

  constructor(
    private http: HttpClient,
    private router: Router,
    private route: ActivatedRoute,
    private translateService: TranslateService,
  ) {}

  ngOnInit(): void {
    this.loadTicket();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  loadTicket(): void {
    const ticketId = this.route.snapshot.paramMap.get('id');
    if (!ticketId) {
      this.error.set('ID du ticket non trouvé');
      return;
    }

    this.loading.set(true);
    this.http.get<Ticket>(`/api/tickets/${ticketId}`).subscribe({
      next: ticket => {
        this.ticket.set(ticket);
        this.loading.set(false);
        this.checkReviewStatus();
      },
      error: () => {
        this.error.set('Erreur lors du chargement du ticket');
        this.loading.set(false);
      },
    });
  }

  // Vérifier le statut de l'avis
  checkReviewStatus(): void {
    const ticket = this.ticket();
    if (!ticket?.id) return;

    // Vérifier si le ticket est fermé
    if (ticket.status === 'Fermé') {
      this.showReviewButton.set(true);

      // Vérifier si un avis existe déjà
      this.clientReviewService.findByTicketId(ticket.id).subscribe({
        next: review => {
          this.hasReview.set(true);
          this.showReviewButton.set(false);
        },
        error: () => {
          // Pas d'avis trouvé, on peut en créer un
          this.hasReview.set(false);
          this.showReviewButton.set(true);
        },
      });
    } else {
      this.showReviewButton.set(false);
    }
  }

  // Rediriger vers le formulaire d'avis (legacy)
  goToReview(): void {
    const ticket = this.ticket();
    if (!ticket?.id) return;

    this.router.navigate(['/client-review', ticket.id]);
  }

  openReviewModal(): void {
    this.showReviewModal.set(true);
  }

  closeReviewModal(): void {
    this.showReviewModal.set(false);
  }

  onReviewSubmitted(): void {
    this.closeReviewModal();
    this.hasReview.set(true);
    this.showReviewButton.set(false);
    this.showSuccessMessage('Merci pour votre avis !');
  }

  getStepIndex(status: string): number {
    const normalized = this.normalizeStatus(status);
    return this.workflowSteps.indexOf(normalized);
  }

  onPaymentCompleted(success: boolean): void {
    if (success) {
      this.showSuccessMessage('Paiement effectué avec succès !');
      this.loadTicket(); // Recharger le ticket pour mettre à jour les statuts
    } else {
      this.showErrorMessage('Erreur lors du paiement. Veuillez réessayer.');
    }
  }

  // Méthodes pour gérer les devis et factures dans les messages
  isDevisMessage(message: string): boolean {
    return message.includes('--- DEVIS ---') || (message.includes('Montant:') && message.includes('MAD'));
  }

  isFactureMessage(message: string): boolean {
    return message.includes('--- FACTURE ---') || message.includes('Facture générée:');
  }

  extractDevisAmount(message: string): string {
    const match = message.match(/Montant:\s*([\d.]+)\s*MAD/);
    return match ? match[1] : '0';
  }

  extractDevisDescription(message: string): string {
    const match = message.match(/Description:\s*(.+?)(?:\n|$)/);
    return match ? match[1].trim() : '';
  }

  extractFactureAmount(message: string): string {
    const match = message.match(/Facture générée:\s*([\d.]+)\s*MAD/);
    return match ? match[1] : '0';
  }

  extractFactureDescription(message: string): string {
    const match = message.match(/Description:\s*(.+?)(?:\n|$)/);
    return match ? match[1].trim() : '';
  }

  canRespondToDevis(): boolean {
    const ticket = this.ticket();
    return ticket ? ticket.status === 'Nouveau' : false;
  }

  canProceedToPayment(): boolean {
    const ticket = this.ticket();
    if (!ticket) return false;
    return ticket.status === 'En attente de paiement' || this.hasInvoiceMessage();
  }

  canValidatePayment(): boolean {
    const ticket = this.ticket();
    if (!ticket) return false;
    return ticket.status === 'En attente de paiement' || this.hasInvoiceMessage();
  }

  acceptDevis(message: string): void {
    if (!this.ticket()) return;

    const amount = this.extractDevisAmount(message);
    const description = this.extractDevisDescription(message);

    this.http
      .put(`/api/tickets/${this.ticket()!.id}/accept-devis`, {
        amount: parseFloat(amount),
        description,
      })
      .subscribe({
        next: () => {
          this.showSuccessMessage('Devis accepté avec succès !');
          this.loadTicket();
        },
        error: () => {
          this.showErrorMessage("Erreur lors de l'acceptation du devis.");
        },
      });
  }

  rejectDevis(message: string): void {
    if (!this.ticket()) return;

    this.http.put(`/api/tickets/${this.ticket()!.id}/reject-devis`, {}).subscribe({
      next: () => {
        this.showSuccessMessage("Devis refusé. L'équipe vous recontactera.");
        this.loadTicket();
      },
      error: () => {
        this.showErrorMessage('Erreur lors du refus du devis.');
      },
    });
  }

  proceedToPayment(message?: string): void {
    if (!this.ticket()) return;

    let amount = 0;
    let description = 'Paiement pour le ticket';

    if (message) {
      amount = parseFloat(this.extractFactureAmount(message));
      description = this.extractFactureDescription(message);
    } else {
      // Si pas de message de facture, utiliser un montant par défaut
      amount = 300; // Montant par défaut
      description = `Paiement pour le ticket #${this.ticket()!.id}`;
    }

    console.warn('Proceeding to payment for amount:', amount, 'MAD');

    // Créer la demande de paiement CMI
    const paymentRequest = {
      ticketId: this.ticket()!.id,
      amount,
      user: 'client', // Utilisateur par défaut pour le client
      description,
    };

    this.http.post('/api/paiements/cmi/initiate', paymentRequest).subscribe({
      next: (response: any) => {
        console.warn('Payment request created:', response);

        if (response.success) {
          // Créer un formulaire pour rediriger vers CMI
          this.submitToCmi(response.gatewayUrl, response.params);
        } else {
          this.showErrorMessage('Erreur lors de la création de la demande de paiement: ' + response.error);
        }
      },
      error: error => {
        console.error('Error creating payment request:', error);
        this.showErrorMessage('Erreur lors de la création de la demande de paiement');
      },
    });
  }

  private submitToCmi(gatewayUrl: string, params: any): void {
    // Créer un formulaire dynamique pour soumettre à CMI
    const form = document.createElement('form');
    form.method = 'POST';
    form.action = gatewayUrl;
    form.target = '_blank';

    // Ajouter tous les paramètres CMI
    Object.keys(params).forEach(key => {
      const input = document.createElement('input');
      input.type = 'hidden';
      input.name = key;
      input.value = params[key];
      form.appendChild(input);
    });

    // Ajouter le formulaire au DOM et le soumettre
    document.body.appendChild(form);
    form.submit();
    document.body.removeChild(form);

    // Redirection silencieuse vers CMI
  }

  // Méthode de test pour vérifier l'API CMI
  testCmiApi(): void {
    console.warn("Test de l'API CMI...");

    // Test de l'endpoint de base
    this.http.get('/api/paiements/test').subscribe({
      next(response) {
        console.warn('Test endpoint de base:', response);
        // Test réussi - pas de message utilisateur
      },
      error: error => {
        console.error('Erreur endpoint de base:', error);
        this.showErrorMessage('API de base non accessible');
      },
    });

    // Test de l'endpoint CMI
    this.http.get('/api/paiements/cmi/test').subscribe({
      next(response: any) {
        console.warn('Test endpoint CMI:', response);
        // Test CMI réussi - pas de message utilisateur
      },
      error: error => {
        console.error('Erreur endpoint CMI:', error);
        this.showErrorMessage('API CMI non accessible');
      },
    });
  }

  getMessageTime(index: number): string {
    // Simuler un horodatage basé sur l'index du message
    const now = new Date();
    const messageTime = new Date(now.getTime() - index * 60000); // 1 minute par message
    return messageTime.toLocaleTimeString('fr-FR', {
      hour: '2-digit',
      minute: '2-digit',
    });
  }

  // Méthodes pour gérer les devis dans la description
  hasDevis(): boolean {
    const description = this.ticket()?.description || '';
    return description.includes('--- DEVIS ---') || description.includes('Montant:');
  }

  extractDevisFromDescription(): any[] {
    const description = this.ticket()?.description || '';
    const devis: any[] = [];

    // Rechercher les sections DEVIS dans la description
    const devisSections = description.split('--- DEVIS ---').filter(section => section.trim());

    devisSections.forEach((section, index) => {
      if (section.trim()) {
        const amountMatch = section.match(/Montant:\s*([\d.]+)\s*MAD/);
        const descriptionMatch = section.match(/Description:\s*(.+?)(?:\n|$)/);

        if (amountMatch) {
          devis.push({
            id: index,
            amount: amountMatch[1],
            description: descriptionMatch ? descriptionMatch[1].trim() : 'Aucune description',
            accepted: false,
            rejected: false,
            acceptedDate: null,
            rejectedDate: null,
          });
        }
      }
    });

    return devis;
  }

  acceptDevisFromDescription(devis: any, index: number): void {
    if (!this.ticket()) return;

    this.http
      .put(
        `/api/tickets/${this.ticket()!.id}/accept-devis`,
        {
          devisIndex: index,
          amount: parseFloat(devis.amount),
          description: devis.description,
        },
        {
          headers: { 'Content-Type': 'application/json' },
        },
      )
      .subscribe({
        next: () => {
          this.showSuccessMessage('Devis accepté avec succès !');
          // Mettre à jour le devis localement
          devis.accepted = true;
          devis.acceptedDate = new Date();
          this.loadTicket();
        },
        error: () => {
          this.showErrorMessage("Erreur lors de l'acceptation du devis.");
        },
      });
  }

  rejectDevisFromDescription(devis: any, index: number): void {
    if (!this.ticket()) return;

    this.http
      .put(
        `/api/tickets/${this.ticket()!.id}/reject-devis`,
        {
          devisIndex: index,
          amount: parseFloat(devis.amount),
          description: devis.description,
        },
        {
          headers: { 'Content-Type': 'application/json' },
        },
      )
      .subscribe({
        next: () => {
          this.showSuccessMessage("Devis refusé. L'équipe vous recontactera.");
          // Mettre à jour le devis localement
          devis.rejected = true;
          devis.rejectedDate = new Date();
          this.loadTicket();
        },
        error: () => {
          this.showErrorMessage('Erreur lors du refus du devis.');
        },
      });
  }

  // Méthodes pour les images
  openImageModal(imageUrl: string): void {
    this.selectedImageUrl.set(imageUrl);
    this.showImageModal.set(true);
  }

  closeImageModal(): void {
    this.showImageModal.set(false);
    this.selectedImageUrl.set(null);
  }

  // Méthodes pour les messages
  sendReply(): void {
    if (!this.ticket() || !this.clientReply.trim()) return;

    this.sendingReply.set(true);
    this.http
      .post(`/api/tickets/${this.ticket()!.id}/messages`, {
        content: this.clientReply.trim(),
      })
      .subscribe({
        next: () => {
          this.clientReply = '';
          this.sendingReply.set(false);
          this.loadTicket(); // Recharger pour avoir les nouveaux messages
        },
        error: () => {
          this.showErrorMessage("Erreur lors de l'envoi de la réponse.");
          this.sendingReply.set(false);
        },
      });
  }

  // Méthodes utilitaires pour les notifications
  private showSuccessMessage(message: string): void {
    this.success.set(message);
    setTimeout(() => {
      this.success.set(null);
    }, 3000);
  }

  private showErrorMessage(message: string): void {
    this.error.set(message);
    setTimeout(() => {
      this.error.set(null);
    }, 5000);
  }

  // Helper methods for translations
  getTicketTypeTranslation(type: string | undefined): string {
    if (!type) return '';

    const typeMap: Record<string, string> = {
      Bug: 'Bug',
      Demande: 'Demande',
      Support: 'Support',
      Autre: 'Autre',
    };

    return typeMap[type] || type;
  }

  getTicketStatusTranslation(status: string | undefined): string {
    if (!status) return '';

    // Normaliser le statut pour gérer les différentes variantes de casse
    const normalizedStatus = status.trim();

    const statusMap: Record<string, string> = {
      Nouveau: 'ticketsClient.status.nouveau',
      'En cours': 'ticketsClient.status.enCours',
      Résolu: 'ticketsClient.status.resolu',
      'En attente de paiement': 'ticketsClient.status.enAttentePaiement',
      'Paiement validé': 'ticketsClient.status.paiementValide',
      Fermé: 'ticketsClient.status.ferme',
      Urgent: 'ticketsClient.status.urgent',
      'DEVIS VALIDÉ': 'ticketsClient.status.devisValide',
      'Devis validé': 'ticketsClient.status.devisValide',
      'DEVIS VALIDE': 'ticketsClient.status.devisValide',
      'Devis Validé': 'ticketsClient.status.devisValide',
    };

    // Chercher d'abord la correspondance exacte
    let translationKey = statusMap[normalizedStatus];

    // Si pas de correspondance exacte, chercher de manière insensible à la casse
    if (!translationKey) {
      const upperStatus = normalizedStatus
        .toUpperCase()
        .normalize('NFD')
        .replace(/[\u0300-\u036f]/g, '');
      if (upperStatus === 'DEVIS VALIDE' || (upperStatus.includes('DEVIS') && upperStatus.includes('VALID'))) {
        translationKey = 'ticketsClient.status.devisValide';
      } else {
        // Pour les autres statuts, utiliser la valeur originale
        translationKey = normalizedStatus;
      }
    }

    return this.translateService.instant(translationKey);
  }

  // Helpers for workflow normalization and detection
  private hasInvoiceMessage(): boolean {
    const messages = this.ticket()?.messageStrings || [];
    return messages.some(m => m.includes('--- FACTURE ---') || /Facture générée/i.test(m));
  }

  private normalizeStatus(status: string): string {
    if (!status) return 'Nouveau';
    // Map backend variants to workflow steps
    const upper = status.toUpperCase();
    if (upper.includes('DEVIS') && upper.includes('VALID')) {
      // If a facture was sent, consider we are waiting for payment
      return this.hasInvoiceMessage() ? 'En attente de paiement' : 'En cours';
    }
    return status;
  }

  validatePayment(ticketId: number): void {
    this.http.put(`/api/tickets/${ticketId}/validate-payment`, {}).subscribe({
      next: () => {
        this.showSuccessMessage('Paiement validé avec succès !');
        this.loadTicket();
      },
      error: () => {
        this.showErrorMessage('Erreur lors de la validation du paiement.');
      },
    });
  }

  // Navigation
  goBack(): void {
    this.router.navigate(['/client-tickets']);
  }
}
