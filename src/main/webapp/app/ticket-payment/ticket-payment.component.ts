import { Component, OnInit, Input, Output, EventEmitter, inject, OnChanges, SimpleChanges } from '@angular/core';
import { PaymentMethodService } from '../payment-methods/payment-method.service';
import { HttpClient } from '@angular/common/http';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { environment } from '../../environments/environment';
import { Router } from '@angular/router';

interface PaymentInfo {
  paymentRequired: boolean;
  amount: number;
  currency: string;
}

interface TicketPaymentStatus {
  paymentRequired: boolean;
  paymentAmount: number;
  paymentCurrency: string;
  paymentStatus: string;
  paymentType: string;
}

@Component({
  selector: 'jhi-ticket-payment',
  templateUrl: './ticket-payment.component.html',
  styleUrls: ['./ticket-payment.component.scss'],
  standalone: true,
  imports: [CommonModule, FormsModule],
})
export class TicketPaymentComponent implements OnInit, OnChanges {
  @Input() ticketId?: number;
  @Input() ticketType?: string;
  @Output() paymentCompleted = new EventEmitter<boolean>();

  paymentInfo?: PaymentInfo;
  paymentStatus?: TicketPaymentStatus;
  isLoading = false;
  errorMessage = '';
  showPaymentForm = false;
  hasAttemptedLoad = false;
  showTransferInfo = false;
  transferProofName: string | null = null;
  /** Fichier sélectionné pour l'upload du justificatif */
  selectedProofFile: File | null = null;
  uploadingProof = false;
  proofUploadSuccess = false;

  private paymentMethodService = inject(PaymentMethodService);
  private router = inject(Router);

  constructor(private http: HttpClient) {}

  ngOnInit(): void {
    this.refreshPaymentData();
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['ticketType'] || changes['ticketId']) {
      this.refreshPaymentData();
    }
  }

  /**
   * Rafraîchir les infos de paiement quand l'ID ou le type du ticket sont disponibles
   */
  private refreshPaymentData(): void {
    // Si les inputs ne sont pas encore fournis, stopper le spinner et éviter de bloquer l'UI
    if (!this.ticketType || !this.ticketId) {
      this.isLoading = false;
      return;
    }

    // Vérifier la nécessité du paiement et récupérer le statut
    this.hasAttemptedLoad = true;
    this.checkPaymentRequired();
    this.getPaymentStatus();
  }

  /**
   * Vérifie si un paiement est requis pour ce type de ticket
   */
  checkPaymentRequired(): void {
    if (!this.ticketType) {
      this.isLoading = false;
      return;
    }

    this.isLoading = true;
    this.http.get<PaymentInfo>(`/api/ticket-payments/check/${this.ticketType}`).subscribe({
      next: info => {
        this.paymentInfo = info;
        this.isLoading = false;
      },
      error: error => {
        console.error('Erreur lors de la vérification du paiement:', error);
        this.errorMessage = 'Erreur lors de la vérification du paiement';
        this.isLoading = false;
      },
    });
  }

  /**
   * Obtient le statut de paiement d'un ticket
   */
  getPaymentStatus(): void {
    if (!this.ticketId) {
      this.isLoading = false;
      return;
    }

    this.isLoading = true;
    this.http.get<TicketPaymentStatus>(`/api/ticket-payments/status/${this.ticketId}`).subscribe({
      next: status => {
        this.paymentStatus = status;
        this.isLoading = false;
      },
      error: error => {
        console.error("Erreur lors de l'obtention du statut de paiement:", error);
        this.errorMessage = "Erreur lors de l'obtention du statut de paiement";
        this.isLoading = false;
      },
    });
  }

  /**
   * Permet d'afficher une zone vide si aucune info n'a été chargée mais que le composant est affiché
   */
  shouldDisplayContent(): boolean {
    // On affiche quand on a tenté de charger ou qu'on a des données
    return this.hasAttemptedLoad || !!this.paymentStatus || !!this.paymentInfo;
  }

  /**
   * Configure un ticket pour nécessiter un paiement
   */
  configurePayment(paymentType: string): void {
    if (!this.ticketId) return;

    this.isLoading = true;
    this.http
      .post(`/api/ticket-payments/configure/${this.ticketId}`, null, {
        params: { paymentType },
      })
      .subscribe({
        next: () => {
          this.getPaymentStatus();
          this.isLoading = false;
        },
        error: error => {
          console.error('Erreur lors de la configuration du paiement:', error);
          this.errorMessage = 'Erreur lors de la configuration du paiement';
          this.isLoading = false;
        },
      });
  }

  /**
   * Affiche le formulaire de paiement
   */
  showPayment(): void {
    this.showPaymentForm = true;
  }

  // Les autres moyens de paiement (CMI / PayPal / méthodes) ont été désactivés côté UI.
  // On conserve uniquement le virement bancaire, affiché directement dans le template.

  /**
   * Gestion du justificatif de virement (capture ou PDF)
   */
  onTransferProofSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files.length > 0) {
      const file = input.files[0];
      this.selectedProofFile = file;
      this.transferProofName = `${file.name} (${Math.round(file.size / 1024)} Ko)`;
      this.proofUploadSuccess = false;
    } else {
      this.selectedProofFile = null;
      this.transferProofName = null;
    }
  }

  /**
   * Envoie le justificatif de paiement au serveur pour que l'admin puisse le consulter.
   */
  uploadProof(): void {
    if (!this.ticketId || !this.selectedProofFile) return;

    this.uploadingProof = true;
    this.errorMessage = '';

    const formData = new FormData();
    formData.append('file', this.selectedProofFile);

    this.http.post(`/api/tickets/${this.ticketId}/payment-proof`, formData).subscribe({
      next: () => {
        this.uploadingProof = false;
        this.proofUploadSuccess = true;
        this.selectedProofFile = null;
        this.transferProofName = null;
        this.errorMessage = '';
        const input = document.getElementById('transferProof') as HTMLInputElement;
        if (input) input.value = '';
        this.paymentCompleted.emit(true);
      },
      error: err => {
        this.uploadingProof = false;
        this.errorMessage = err.error?.message || err.message || "Erreur lors de l'envoi du justificatif.";
      },
    });
  }

  private getPaymentAmount(): number {
    if (this.paymentStatus?.paymentAmount) return this.paymentStatus.paymentAmount;
    if (this.paymentInfo?.amount) return this.paymentInfo.amount;
    return 0;
  }

  private getPaymentCurrency(): string {
    if (this.paymentStatus?.paymentCurrency) return this.paymentStatus.paymentCurrency;
    if (this.paymentInfo?.currency) return this.paymentInfo.currency;
    return 'MAD';
  }

  /**
   * Masque le formulaire de paiement
   */
  hidePayment(): void {
    this.showPaymentForm = false;
  }

  /**
   * Obtient le texte du statut de paiement
   */
  getPaymentStatusText(status: string | undefined): string {
    if (!status) return 'Inconnu';

    switch (status) {
      case 'PENDING':
        return 'En attente de paiement';
      case 'COMPLETED':
        return 'Paiement complété';
      case 'FAILED':
        return 'Paiement échoué';
      default:
        return status;
    }
  }

  /**
   * Obtient la classe CSS pour le statut de paiement
   */
  getPaymentStatusClass(status: string | undefined): string {
    if (!status) return 'status-unknown';

    switch (status) {
      case 'PENDING':
        return 'status-pending';
      case 'COMPLETED':
        return 'status-completed';
      case 'FAILED':
        return 'status-failed';
      default:
        return 'status-unknown';
    }
  }
}
