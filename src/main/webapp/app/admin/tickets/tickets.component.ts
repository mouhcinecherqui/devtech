import { Component, OnInit, ChangeDetectorRef, inject } from '@angular/core';
import { HttpClient, HttpResponse } from '@angular/common/http';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { NgbPaginationModule } from '@ng-bootstrap/ng-bootstrap';
import SharedModule from '../../shared/shared.module';
import { TicketPaymentComponent } from '../../ticket-payment/ticket-payment.component';
import { AutoRefreshService } from '../../core/services/auto-refresh.service';
import { NotificationService } from '../../core/services/notification.service';
import ItemCountComponent from '../../shared/pagination/item-count.component';
import { createRequestOption } from '../../core/request/request-util';
import { Pagination } from '../../core/request/request.model';
import { AlertService } from '../../core/util/alert.service';

interface Ticket {
  id?: number;
  type: string;
  description: string;
  clientName?: string;
  clientEmail?: string;
  backofficeUrl?: string;
  backofficeLogin?: string;
  backofficePassword?: string;
  hostingUrl?: string;
  createdDate?: string;
  status?: string;
  imageUrl?: string;
  /** URL du justificatif de paiement (virement) déposé par le client */
  paymentProofFileUrl?: string;
  messages?: string[];
  messageStrings?: string[];
}

@Component({
  selector: 'jhi-admin-tickets',
  templateUrl: './tickets.component.html',
  styleUrls: ['./tickets.component.scss'],
  standalone: true,
  imports: [CommonModule, FormsModule, SharedModule, TicketPaymentComponent, NgbPaginationModule, ItemCountComponent],
})
export class TicketsComponent implements OnInit {
  tickets: Ticket[] = [];
  loading = false;
  error: string | null = null;
  page = 1;
  itemsPerPage = 10;
  totalItems = 0;

  showDetailModal = false;
  selectedTicket: Ticket | null = null;
  newMessage = '';

  // Propriétés pour la modal d'image
  showImageModal = false;
  selectedImageUrl: string | null = null;

  /** Chargement du justificatif de paiement (requête authentifiée) */
  loadingPaymentProof = false;

  // Propriétés pour la modal de création
  showCreateModal = false;
  creating = false;
  newTicket: any = {
    type: '',
    description: '',
    status: 'Nouveau',
    clientEmail: '',
  };

  // Propriétés pour la gestion des devis
  showDevisModal = false;
  selectedTicketForDevis: Ticket | null = null;
  devisAmount = 0;
  devisDescription = '';
  submittingDevis = false;

  private readonly http = inject(HttpClient);
  private readonly cdr = inject(ChangeDetectorRef);
  private readonly autoRefreshService = inject(AutoRefreshService);
  private readonly alertService = inject(AlertService);
  private readonly notificationService = inject(NotificationService);

  ngOnInit(): void {
    this.fetchTickets();

    // Configurer l'actualisation automatique toutes les 30 secondes
    this.autoRefreshService.setRefreshInterval(30000);
    this.autoRefreshService.refreshTrigger$.subscribe(() => {
      this.fetchTickets();
    });
  }

  fetchTickets(): void {
    this.loading = true;

    // Timeout pour éviter les blocages
    const timeoutId = setTimeout(() => {
      console.warn('Timeout API, utilisation des données de test');
      this.loadFallbackData();
    }, 3000);

    const req = {
      page: this.page - 1,
      size: this.itemsPerPage,
      sort: ['id,desc'],
    };

    const options = createRequestOption(req);

    this.http.get<Ticket[]>('/api/tickets', { params: options, observe: 'response' }).subscribe({
      next: response => {
        clearTimeout(timeoutId);
        console.log('Tickets chargés:', response.body);

        // Si l'API retourne une liste vide, utiliser les données de test
        if (!response.body || response.body.length === 0) {
          console.warn('API retourne une liste vide, utilisation des données de test');
          this.loadFallbackData();
        } else {
          this.tickets = response.body;
          this.totalItems = parseInt(response.headers.get('X-Total-Count') || '0', 10);
        }
        this.loading = false;
        this.cdr.detectChanges(); // Forcer l'actualisation de l'interface
      },
      error: error => {
        clearTimeout(timeoutId);
        console.warn('Erreur API, utilisation des données de test:', error);
        this.loadFallbackData();
        this.loading = false;
        this.cdr.detectChanges(); // Forcer l'actualisation de l'interface
      },
    });
  }

  onPageChange(page: number): void {
    this.page = page;
    this.fetchTickets();
  }

  private loadFallbackData(): void {
    this.tickets = [];
    // Ne pas afficher d'alerte bloquante côté UI, on laisse l'état vide + console
    this.error = null;
    this.cdr.detectChanges();
  }

  openDetailModal(ticket: Ticket): void {
    this.showDetailModal = true;
    this.selectedTicket = { ...ticket, messageStrings: ticket.messageStrings || [] };
    // Recharger le ticket depuis l'API pour avoir les données à jour (ex. justificatif de paiement)
    if (ticket.id) {
      this.http.get<Ticket>(`/api/tickets/${ticket.id}`).subscribe({
        next: fresh => {
          this.selectedTicket = { ...fresh, messageStrings: fresh.messageStrings || this.selectedTicket?.messageStrings || [] };
          this.cdr.detectChanges();
        },
        error: () => {},
      });
    }
  }

  closeDetailModal(): void {
    this.showDetailModal = false;
    this.selectedTicket = null;
    this.newMessage = '';
  }

  // Méthodes pour la modal d'image
  openImageModal(imageUrl: string): void {
    this.selectedImageUrl = imageUrl;
    this.showImageModal = true;
  }

  closeImageModal(): void {
    if (this.selectedImageUrl?.startsWith('blob:')) {
      URL.revokeObjectURL(this.selectedImageUrl);
    }
    this.showImageModal = false;
    this.selectedImageUrl = null;
  }

  /**
   * Ouvre le justificatif de paiement via une requête authentifiée (évite le 401 en nouvel onglet).
   * Affiche l'image en modal, ou ouvre PDF/autre dans un nouvel onglet.
   */
  openPaymentProof(url: string): void {
    if (!url) return;
    this.loadingPaymentProof = true;
    this.http.get(url, { responseType: 'blob' }).subscribe({
      next: blob => {
        this.loadingPaymentProof = false;
        const blobUrl = URL.createObjectURL(blob);
        // Détecter image par Content-Type ou par extension dans l'URL
        const isImageByType = blob.type.startsWith('image/');
        const isImageByUrl = /\.(png|jpe?g|gif|webp|bmp)(\?|$)/i.test(url);
        const isImage = isImageByType || isImageByUrl;
        if (isImage) {
          this.selectedImageUrl = blobUrl;
          this.showImageModal = true;
          this.cdr.detectChanges();
        } else {
          const w = window.open(blobUrl, '_blank');
          if (w) setTimeout(() => URL.revokeObjectURL(blobUrl), 30000);
          else URL.revokeObjectURL(blobUrl);
        }
      },
      error: err => {
        this.loadingPaymentProof = false;
        const is404 = err.status === 404;
        const message = is404
          ? "Le justificatif n'est plus disponible (fichier perdu après redémarrage du serveur). Demandez au client de le renvoyer."
          : 'Impossible de charger le justificatif. Vérifiez votre connexion.';
        this.alertService.addAlert({
          type: 'danger',
          message,
          timeout: 8000,
        });
      },
    });
  }

  saveTicket(): void {
    if (!this.selectedTicket) return;

    const showSuccess = (): void => {
      this.alertService.addAlert({
        type: 'success',
        message: 'Enregistrement réussi. Le ticket a été mis à jour.',
        timeout: 4000,
        toast: true,
        position: 'top right',
      });
    };

    const showError = (message: string): void => {
      this.alertService.addAlert({
        type: 'danger',
        message,
        timeout: 5000,
        toast: true,
        position: 'top right',
      });
    };

    // Mettre à jour le statut du ticket
    this.http.put<Ticket>(`/api/tickets/${this.selectedTicket.id}`, this.selectedTicket).subscribe({
      next: updated => {
        const idx = this.tickets.findIndex(t => t.id === updated.id);
        if (idx !== -1) this.tickets[idx] = updated;
        this.selectedTicket = { ...updated };

        // Ajouter le message si présent via l'endpoint dédié
        if (this.newMessage && this.newMessage.trim() && this.selectedTicket) {
          this.http
            .post<any>(`/api/tickets/${this.selectedTicket.id}/messages`, {
              content: this.newMessage.trim(),
            })
            .subscribe({
              next: () => {
                this.notificationService.forceRefresh();
                // Recharger les messages du ticket
                if (this.selectedTicket) {
                  this.http.get<string[]>(`/api/tickets/${this.selectedTicket.id}/messages`).subscribe({
                    next: messages => {
                      if (this.selectedTicket) {
                        this.selectedTicket.messageStrings = messages;
                        // Mettre à jour le ticket dans la liste
                        if (idx !== -1) {
                          this.tickets[idx].messageStrings = messages;
                        }
                      }
                    },
                  });
                }
                this.newMessage = '';
                showSuccess();
              },
              error: () => {
                showError("Erreur lors de l'enregistrement : l'ajout du message a échoué.");
              },
            });
        } else {
          this.notificationService.forceRefresh();
          this.newMessage = '';
          showSuccess();
        }
      },
      error: () => {
        showError("Erreur lors de l'enregistrement : la mise à jour du ticket a échoué.");
      },
    });
  }

  sendMessage(): void {
    if (!this.selectedTicket || !this.newMessage.trim()) return;

    console.warn('Envoi du message:', this.newMessage.trim());
    console.warn('URL:', `/api/tickets/${this.selectedTicket.id}/messages`);

    this.http
      .post<any>(`/api/tickets/${this.selectedTicket.id}/messages`, {
        content: this.newMessage.trim(),
      })
      .subscribe({
        next: response => {
          this.notificationService.forceRefresh();
          console.warn('Message envoyé avec succès:', response);
          if (this.selectedTicket) {
            this.selectedTicket.messageStrings = this.selectedTicket.messageStrings || [];
            this.selectedTicket.messageStrings.push('[ADMIN] ' + this.newMessage.trim());
            this.newMessage = '';
          }
        },
        error: error => {
          console.error("Erreur lors de l'envoi du message:", error);
          console.error('Status:', error.status);
          console.error('Message:', error.message || 'No message available');
          console.error('Response:', error.error);
          this.error = "Erreur lors de l'envoi du message: " + (error.message || 'Erreur inconnue');
        },
      });
  }

  onPaymentCompleted(success: boolean): void {
    if (success) {
      // Recharger les tickets pour mettre à jour les statuts
      this.fetchTickets();
      // Fermer la modal de détail
      this.closeDetailModal();
    } else {
      this.error = 'Erreur lors du paiement. Veuillez réessayer.';
    }
  }

  // Méthodes pour la création de tickets
  openCreateTicketModal(): void {
    this.showCreateModal = true;
    this.newTicket = {
      type: '',
      description: '',
      status: 'Nouveau',
      clientEmail: '',
    };
  }

  closeCreateTicketModal(): void {
    this.showCreateModal = false;
    this.creating = false;
    this.newTicket = {
      type: '',
      description: '',
      status: 'Nouveau',
      clientEmail: '',
    };
  }

  createTicket(): void {
    if (!this.newTicket.type || !this.newTicket.description) {
      return;
    }

    this.creating = true;

    const ticketData = {
      type: this.newTicket.type,
      description: this.newTicket.description,
      status: this.newTicket.status,
      clientEmail: this.newTicket.clientEmail || null,
    };

    this.http.post<Ticket>('/api/tickets', ticketData).subscribe({
      next: createdTicket => {
        this.notificationService.forceRefresh();
        this.tickets.unshift(createdTicket); // Ajouter au début de la liste
        this.closeCreateTicketModal();
        this.alertService.addAlert({
          type: 'success',
          message: 'Ticket créé avec succès !',
          timeout: 3000,
        });
      },
      error: error => {
        this.creating = false;
        console.error('Erreur lors de la création du ticket:', error);
        this.alertService.addAlert({
          type: 'danger',
          message: 'Erreur lors de la création du ticket. Veuillez réessayer.',
          timeout: 5000,
        });
      },
    });
  }

  // Méthodes pour la gestion des devis
  openDevisModal(ticket: Ticket): void {
    this.selectedTicketForDevis = ticket;
    this.showDevisModal = true;
    this.devisAmount = 0;
    this.devisDescription = '';
  }

  closeDevisModal(): void {
    this.showDevisModal = false;
    this.selectedTicketForDevis = null;
    this.devisAmount = 0;
    this.devisDescription = '';
    this.submittingDevis = false;
  }

  submitDevis(): void {
    if (!this.selectedTicketForDevis || this.devisAmount <= 0) {
      this.alertService.addAlert({
        type: 'warning',
        message: 'Veuillez saisir un montant valide pour le devis.',
        timeout: 5000,
      });
      return;
    }

    this.submittingDevis = true;
    const devisData = {
      ticketId: this.selectedTicketForDevis.id,
      amount: this.devisAmount,
      description: this.devisDescription,
      status: 'PENDING_VALIDATION',
    };

    this.http.post('/api/tickets/devis', devisData).subscribe({
      next: () => {
        this.notificationService.forceRefresh();
        this.alertService.addAlert({
          type: 'success',
          message: 'Devis envoyé avec succès !',
          timeout: 3000,
        });
        this.closeDevisModal();
        this.fetchTickets(); // Recharger les tickets pour mettre à jour les statuts
      },
      error: error => {
        this.submittingDevis = false;
        console.error("Erreur lors de l'envoi du devis:", error);
        this.alertService.addAlert({
          type: 'danger',
          message: "Erreur lors de l'envoi du devis. Veuillez réessayer.",
          timeout: 5000,
        });
      },
    });
  }

  // Méthodes utilitaires pour vérifier les étapes du workflow
  canCreateDevis(ticket: Ticket): boolean {
    return ticket.status === 'Nouveau';
  }

  canStartWork(ticket: Ticket): boolean {
    return ticket.status === 'Nouveau';
  }

  canResolve(ticket: Ticket): boolean {
    return ticket.status === 'En cours';
  }

  canSendInvoice(ticket: Ticket): boolean {
    return ticket.status === 'Résolu';
  }

  canValidatePayment(ticket: Ticket): boolean {
    return ticket.status === 'En attente de paiement';
  }

  canClose(ticket: Ticket): boolean {
    return ticket.status === 'Paiement validé';
  }

  // Méthode de test pour vérifier les permissions
  testPermissions(): void {
    this.http.get('/api/tickets/test-permissions').subscribe({
      next: (response: any) => {
        console.warn('Test des permissions:', response);
        this.alertService.addAlert({
          type: 'info',
          message: `Permissions testées: Login: ${response.login}, Autorités: ${response.authorities.join(', ')}, Est admin: ${response.isAdmin}`,
          timeout: 5000,
        });
      },
      error: error => {
        console.error('Erreur lors du test des permissions:', error);
        this.alertService.addAlert({
          type: 'danger',
          message: `Erreur lors du test des permissions: ${error.status} - ${error.message || 'Erreur inconnue'}`,
          timeout: 5000,
        });
      },
    });
  }

  // Méthode de diagnostic pour vérifier l'état de la base de données
  runDiagnostic(): void {
    this.http.get('/api/tickets/diagnostic').subscribe({
      next: (response: any) => {
        console.warn('Diagnostic:', response);
        let diagnosticMessage = 'Diagnostic de la base de données: ';
        diagnosticMessage += `Connexion DB: ${response.databaseConnected ? 'OK' : 'ERREUR'}, `;
        diagnosticMessage += `Utilisateur: ${response.currentUser}, `;
        diagnosticMessage += `Est admin: ${response.isAdmin}, `;
        diagnosticMessage += `Nombre de tickets: ${response.ticketCount}, `;
        diagnosticMessage += `Table messages existe: ${response.messageTableExists ? 'OUI' : 'NON'}, `;
        diagnosticMessage += `Nombre de messages: ${response.messageCount}`;

        if (response.messageTableError) {
          diagnosticMessage += `, Erreur table messages: ${response.messageTableError}`;
        }

        this.alertService.addAlert({
          type: 'info',
          message: diagnosticMessage,
          timeout: 10000,
        });
      },
      error: error => {
        console.error('Erreur lors du diagnostic:', error);
        this.alertService.addAlert({
          type: 'danger',
          message: `Erreur lors du diagnostic: ${error.status} - ${error.message || 'Erreur inconnue'}`,
          timeout: 5000,
        });
      },
    });
  }

  // Méthode pour obtenir l'heure du message
  getMessageTime(index: number): string {
    // Simuler un horodatage basé sur l'index du message
    const now = new Date();
    const messageTime = new Date(now.getTime() - index * 60000); // 1 minute par message
    return messageTime.toLocaleTimeString('fr-FR', {
      hour: '2-digit',
      minute: '2-digit',
    });
  }

  // Méthode pour envoyer la validation du paiement au client
  sendPaymentValidationToClient(ticket: Ticket): void {
    if (!ticket) return;

    // Extraire le montant du devis depuis la description
    const devisMatch = ticket.description.match(/Montant:\s*([\d.]+)\s*MAD/);
    const amount = devisMatch ? parseFloat(devisMatch[1]) : 0;

    // Créer le message de validation du paiement
    const paymentValidationMessage = `--- FACTURE ---\nFacture générée: ${amount} MAD\nDescription: Facture pour le ticket #${ticket.id}\nLe client peut maintenant procéder au paiement via CMI.`;

    // Envoyer le message au client
    console.warn('Envoi du message de validation du paiement:', paymentValidationMessage);
    console.warn('URL:', `/api/tickets/${ticket.id}/messages`);

    this.http
      .post(`/api/tickets/${ticket.id}/messages`, {
        content: paymentValidationMessage,
      })
      .subscribe({
        next: response => {
          this.notificationService.forceRefresh();
          console.warn('Message de validation du paiement envoyé avec succès:', response);

          // Changer le statut du ticket vers "En attente de paiement"
          ticket.status = 'En attente de paiement';

          // Mettre à jour le ticket
          this.http.put<Ticket>(`/api/tickets/${ticket.id}`, ticket).subscribe({
            next: updatedTicket => {
              // Mettre à jour le ticket dans la liste
              const idx = this.tickets.findIndex(t => t.id === updatedTicket.id);
              if (idx !== -1) {
                this.tickets[idx] = updatedTicket;
              }

              this.alertService.addAlert({
                type: 'success',
                message: 'Facture envoyée au client avec succès ! Le ticket est maintenant en attente de paiement.',
                timeout: 5000,
              });
            },
            error: updateError => {
              console.error('Erreur lors de la mise à jour du ticket:', updateError);
              this.alertService.addAlert({
                type: 'warning',
                message: 'Facture envoyée mais erreur lors de la mise à jour du statut du ticket.',
                timeout: 5000,
              });
            },
          });
        },
        error: error => {
          console.error("Erreur lors de l'envoi de la validation du paiement:", error);
          console.error('Status:', error.status);
          console.error('Message:', error.message || 'No message available');
          console.error('Response:', error.error);

          // Afficher un message d'erreur plus informatif
          let errorMessage = "Erreur lors de l'envoi de la validation du paiement";
          if (error.status === 500) {
            errorMessage +=
              ': Erreur interne du serveur. Veuillez vérifier que la base de données est accessible et que la table ticket_message existe.';
          } else if (error.status === 403) {
            errorMessage += ": Permission refusée. Veuillez vérifier vos droits d'accès.";
          } else if (error.status === 404) {
            errorMessage += ': Ticket non trouvé.';
          } else {
            errorMessage += `: ${error.status} - ${error.message || 'Erreur inconnue'}`;
          }

          this.alertService.addAlert({
            type: 'danger',
            message: errorMessage,
            timeout: 5000,
          });
        },
      });
  }

  validatePayment(ticket: Ticket): void {
    if (!ticket?.id) return;

    this.http.put(`/api/tickets/${ticket.id}/validate-payment`, {}).subscribe({
      next: () => {
        this.notificationService.forceRefresh();
        // Mettre à jour le statut localement
        ticket.status = 'Paiement validé';

        // Mettre à jour le ticket dans la liste
        const idx = this.tickets.findIndex(t => t.id === ticket.id);
        if (idx !== -1) {
          this.tickets[idx] = ticket;
        }

        this.alertService.addAlert({
          type: 'success',
          message: 'Paiement validé avec succès !',
          timeout: 3000,
        });
      },
      error: error => {
        console.error('Erreur lors de la validation du paiement:', error);
        this.alertService.addAlert({
          type: 'danger',
          message: 'Erreur lors de la validation du paiement',
          timeout: 5000,
        });
      },
    });
  }
}
