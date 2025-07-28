import { Component, OnInit } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import SharedModule from '../../shared/shared.module';
import { TicketPaymentComponent } from '../../ticket-payment/ticket-payment.component';

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
  selector: 'app-admin-tickets',
  templateUrl: './tickets.component.html',
  styleUrls: ['./tickets.component.scss'],
  standalone: true,
  imports: [CommonModule, FormsModule, SharedModule, TicketPaymentComponent],
})
export class TicketsComponent implements OnInit {
  tickets: Ticket[] = [];
  loading = false;
  error: string | null = null;

  showDetailModal = false;
  selectedTicket: Ticket | null = null;
  newMessage: string = '';

  // Propriétés pour la modal d'image
  showImageModal = false;
  selectedImageUrl: string | null = null;

  constructor(private http: HttpClient) {}

  ngOnInit(): void {
    this.fetchTickets();
  }

  fetchTickets(): void {
    this.loading = true;
    this.http.get<Ticket[]>('/api/tickets').subscribe({
      next: tickets => {
        this.tickets = tickets;
        this.loading = false;
      },
      error: () => {
        this.error = 'Erreur lors du chargement des tickets';
        this.loading = false;
      },
    });
  }

  openDetailModal(ticket: Ticket): void {
    this.selectedTicket = { ...ticket, messageStrings: ticket.messageStrings || [] };
    this.showDetailModal = true;
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
    this.showImageModal = false;
    this.selectedImageUrl = null;
  }

  saveTicket(): void {
    if (!this.selectedTicket) return;

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
              },
              error: () => {
                alert("Erreur lors de l'ajout du message");
              },
            });
        }

        this.newMessage = '';
        alert('Ticket mis à jour avec succès');
      },
      error: () => {
        alert('Erreur lors de la mise à jour du ticket');
      },
    });
  }

  addTestMessage(): void {
    if (!this.selectedTicket || !this.newMessage.trim()) return;

    this.http
      .post<any>(`/api/tickets/${this.selectedTicket.id}/messages`, {
        content: this.newMessage.trim(),
      })
      .subscribe({
        next: () => {
          if (this.selectedTicket) {
            this.selectedTicket.messageStrings = this.selectedTicket.messageStrings || [];
            this.selectedTicket.messageStrings.push(this.newMessage.trim());
            this.newMessage = '';
          }
        },
        error: () => {
          this.error = "Erreur lors de l'ajout du message";
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
}
