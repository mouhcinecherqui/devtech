import { Component, OnInit, signal, ViewChild, ElementRef, ChangeDetectorRef, computed } from '@angular/core';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule, FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { HttpClient, HttpEvent, HttpEventType } from '@angular/common/http';
import { TranslateService } from '@ngx-translate/core';
import SharedModule from '../shared/shared.module';

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
  selector: 'app-client-tickets',
  templateUrl: './client-tickets.component.html',
  styleUrls: ['./client-tickets.component.scss'],
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, FormsModule, SharedModule],
})
export class ClientTicketsComponent implements OnInit {
  tickets = signal<Ticket[]>([]);
  loading = signal(false);
  error = signal<string | null>(null);
  success = signal<string | null>(null);
  ticketForm: FormGroup;
  ticketTypes = ['Bug', 'Demande', 'Support', 'Autre'];
  showModal = signal(false);
  workflowSteps = ['Nouveau', 'En cours', 'Résolu', 'Fermé'];
  statusFilter = signal<string>('');

  selectedTicket: Ticket | null = null;
  showDetailModal = signal(false);

  // Image upload signals
  selectedImagePreview = signal<string | null>(null);
  uploadProgress = signal<number>(0);
  selectedFile: File | null = null;

  // Image modal signals
  showImageModal = signal<boolean>(false);
  selectedImageUrl = signal<string | null>(null);

  // Client reply - utiliser une propriété normale au lieu d'un signal
  clientReply: string = '';
  sendingReply = signal<boolean>(false);

  // Computed properties for filtered tickets and statistics
  filteredTickets = computed(() => {
    const filter = this.statusFilter();
    if (!filter) return this.tickets();
    return this.tickets().filter(ticket => ticket.status === filter);
  });

  getStepIndex(status: string): number {
    return this.workflowSteps.indexOf(status);
  }

  getOpenTickets(): number {
    return this.tickets().filter(ticket => ticket.status === 'Nouveau' || ticket.status === 'En cours' || ticket.status === 'Urgent')
      .length;
  }

  getResolvedTickets(): number {
    return this.tickets().filter(ticket => ticket.status === 'Résolu' || ticket.status === 'Fermé').length;
  }

  filterTickets(): void {
    // This method is called when the filter changes
    // The filtering is handled by the computed property
  }

  openDetailModal(ticket: Ticket): void {
    this.selectedTicket = ticket;
    this.showDetailModal.set(true);

    // Recharger les messages si ils ne sont pas présents
    if (!ticket.messageStrings || ticket.messageStrings.length === 0) {
      this.http.get<string[]>(`/api/tickets/${ticket.id}/messages`).subscribe({
        next: messages => {
          if (this.selectedTicket && this.selectedTicket.id === ticket.id) {
            this.selectedTicket.messageStrings = messages;
          }
        },
        error: error => {
          console.error('Erreur lors du chargement des messages:', error);
        },
      });
    }
  }

  closeDetailModal(): void {
    this.showDetailModal.set(false);
    this.selectedTicket = null;
    this.clientReply = ''; // Reset reply when closing modal
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
    this.clearErrorAfterDelay(3000);
  }

  // Méthode pour effacer automatiquement les messages d'erreur
  private clearErrorAfterDelay(delay: number = 5000): void {
    setTimeout(() => {
      this.error.set(null);
    }, delay);
  }

  // Méthode pour effacer le message de réponse
  private clearReplyMessage(): void {
    console.log('Avant effacement:', this.clientReply);
    this.clientReply = '';
    console.log('Après effacement:', this.clientReply);
    this.cdr.detectChanges();

    // Forcer une deuxième détection de changements après un court délai
    setTimeout(() => {
      this.cdr.detectChanges();
    }, 10);
  }

  // Méthode pour envoyer la réponse du client
  sendReply(): void {
    if (!this.selectedTicket || !this.clientReply.trim()) return;

    console.log('Début sendReply, message:', this.clientReply);
    this.sendingReply.set(true);

    // Sauvegarder le contenu du message avant de l'envoyer
    const messageContent = this.clientReply.trim();

    // Effacer le message immédiatement pour une meilleure UX
    this.clearReplyMessage();

    // Utiliser setTimeout avec délai 0 pour forcer la mise à jour de l'interface
    setTimeout(() => {
      // Vérifier que le ticket est toujours sélectionné
      if (!this.selectedTicket) {
        this.sendingReply.set(false);
        return;
      }

      console.log('Envoi du message:', messageContent);

      // Utiliser l'endpoint dédié pour ajouter un message
      this.http
        .post<any>(`/api/tickets/${this.selectedTicket.id}/messages`, {
          content: messageContent,
        })
        .subscribe({
          next: response => {
            console.log('Message envoyé avec succès:', response);

            // Ajouter le message directement à l'interface
            if (this.selectedTicket) {
              this.selectedTicket.messageStrings = this.selectedTicket.messageStrings || [];
              this.selectedTicket.messageStrings.push(`[CLIENT] ${messageContent}`);
            }

            // Réinitialiser l'état d'envoi
            this.sendingReply.set(false);

            // Forcer la détection de changements
            this.cdr.detectChanges();

            // Notification de succès
            this.translateService.get('ticketsClient.replySent').subscribe((message: string) => {
              this.showSuccessMessage(message);
            });

            // Rafraîchir la page après un délai pour s'assurer que tout est à jour
            setTimeout(() => {
              window.location.reload();
            }, 1000);
          },
          error: error => {
            console.error("Erreur lors de l'envoi de la réponse:", error);
            this.sendingReply.set(false);

            // Remettre le message dans le champ en cas d'erreur
            this.clientReply = messageContent;

            // Forcer la mise à jour de l'interface
            this.cdr.detectChanges();

            this.translateService.get('ticketsClient.replyError').subscribe((message: string) => {
              this.showErrorMessage(message);
            });
          },
        });
    }, 0); // Délai 0 pour forcer la mise à jour de l'interface
  }

  // Image upload methods
  onImageSelected(event: any): void {
    const file = event.target.files[0];
    if (file) {
      // Validate file type
      if (!file.type.startsWith('image/')) {
        this.error.set('Veuillez sélectionner une image valide');
        return;
      }

      // Validate file size (5MB max)
      if (file.size > 5 * 1024 * 1024) {
        this.error.set("L'image ne doit pas dépasser 5MB");
        return;
      }

      this.selectedFile = file;
      this.uploadProgress.set(0);

      // Create preview
      const reader = new FileReader();
      reader.onload = (e: any) => {
        this.selectedImagePreview.set(e.target.result);
      };
      reader.readAsDataURL(file);
    }
  }

  removeImage(event: Event): void {
    event.stopPropagation();
    this.selectedFile = null;
    this.selectedImagePreview.set(null);
    this.uploadProgress.set(0);
    this.error.set(null);
  }

  openImageModal(imageUrl: string): void {
    this.selectedImageUrl.set(imageUrl);
    this.showImageModal.set(true);
  }

  closeImageModal(): void {
    this.showImageModal.set(false);
    this.selectedImageUrl.set(null);
  }

  // Helper methods for translations
  getTicketTypeTranslation(type: string | undefined): string {
    if (!type) return '';

    const typeMap: { [key: string]: string } = {
      Bug: 'Bug',
      Demande: 'Demande',
      Support: 'Support',
      Autre: 'Autre',
    };

    return typeMap[type] || type;
  }

  getTicketStatusTranslation(status: string | undefined): string {
    if (!status) return '';

    const statusMap: { [key: string]: string } = {
      Nouveau: 'Nouveau',
      'En cours': 'En cours',
      Résolu: 'Résolu',
      Fermé: 'Fermé',
      Urgent: 'Urgent',
    };

    return statusMap[status] || status;
  }

  @ViewChild('typeInput') typeInput?: ElementRef;

  constructor(
    private http: HttpClient,
    private fb: FormBuilder,
    private cdr: ChangeDetectorRef,
    private translateService: TranslateService,
  ) {
    this.ticketForm = this.fb.group({
      type: ['', Validators.required],
      description: ['', Validators.required],
      backofficeUrl: [''],
      backofficeLogin: [''],
      backofficePassword: [''],
      hostingUrl: [''],
      image: [''],
    });
  }

  ngOnInit(): void {
    this.fetchTickets();
  }

  fetchTickets(): void {
    this.loading.set(true);
    this.http.get<Ticket[]>('/api/tickets').subscribe({
      next: tickets => {
        this.tickets.set(tickets);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Erreur lors du chargement des tickets');
        this.loading.set(false);
        this.clearErrorAfterDelay();
      },
    });
  }

  openModal(): void {
    this.showModal.set(true);
    this.cdr.detectChanges();
    setTimeout(() => {
      this.typeInput?.nativeElement.focus();
    }, 100);
  }

  closeModal(): void {
    this.showModal.set(false);
    this.ticketForm.reset();
    this.error.set(null);
    this.selectedFile = null;
    this.selectedImagePreview.set(null);
    this.uploadProgress.set(0);
  }

  submit(): void {
    if (this.ticketForm.invalid) return;

    this.loading.set(true);
    this.uploadProgress.set(0);
    // Effacer les messages d'erreur précédents
    this.error.set(null);

    // Create FormData for file upload
    const formData = new FormData();
    formData.append('type', this.ticketForm.value.type);
    formData.append('description', this.ticketForm.value.description);
    formData.append('backofficeUrl', this.ticketForm.value.backofficeUrl || '');
    formData.append('backofficeLogin', this.ticketForm.value.backofficeLogin || '');
    formData.append('backofficePassword', this.ticketForm.value.backofficePassword || '');
    formData.append('hostingUrl', this.ticketForm.value.hostingUrl || '');

    if (this.selectedFile) {
      formData.append('image', this.selectedFile);
    }

    // Upload with progress tracking
    this.http
      .post<Ticket>('/api/tickets', formData, {
        reportProgress: true,
        observe: 'events',
      })
      .subscribe({
        next: (event: HttpEvent<any>) => {
          if (event.type === HttpEventType.UploadProgress) {
            const progress = Math.round((100 * event.loaded) / (event.total || 1));
            this.uploadProgress.set(progress);
          } else if (event.type === HttpEventType.Response) {
            const ticket = event.body;
            this.tickets.set([ticket, ...this.tickets()]);
            this.closeModal();
            this.loading.set(false);
            this.uploadProgress.set(100);

            // Afficher un message de succès
            this.showSuccessMessage('Ticket créé avec succès !');
          }
        },
        error: () => {
          this.error.set('Erreur lors de la création du ticket');
          this.loading.set(false);
          this.uploadProgress.set(0);
          this.clearErrorAfterDelay();
        },
      });
  }
}
