import { Component, OnInit } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

interface Ticket {
  id: number;
  type: string;
  description: string;
  backofficeUrl?: string;
  backofficeLogin?: string;
  backofficePassword?: string;
  hostingUrl?: string;
  createdDate?: string;
  status?: string;
  messages?: string[];
}

@Component({
  selector: 'app-admin-tickets',
  templateUrl: './tickets.component.html',
  styleUrls: ['./tickets.component.scss'],
  standalone: true,
  imports: [CommonModule, FormsModule],
})
export class TicketsComponent implements OnInit {
  tickets: Ticket[] = [];
  loading = false;
  error: string | null = null;

  showDetailModal = false;
  selectedTicket: Ticket | null = null;
  newMessage: string = '';

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
    this.selectedTicket = { ...ticket, messages: ticket.messages || [] };
    this.showDetailModal = true;
  }

  closeDetailModal(): void {
    this.showDetailModal = false;
    this.selectedTicket = null;
    this.newMessage = '';
  }

  saveTicket(): void {
    if (!this.selectedTicket) return;
    // Ajoute le message si présent
    if (this.newMessage && this.newMessage.trim()) {
      this.selectedTicket.messages = this.selectedTicket.messages || [];
      this.selectedTicket.messages.push(this.newMessage.trim());
    }
    this.http.put<Ticket>(`/api/tickets/${this.selectedTicket.id}`, this.selectedTicket).subscribe({
      next: updated => {
        const idx = this.tickets.findIndex(t => t.id === updated.id);
        if (idx !== -1) this.tickets[idx] = updated;
        this.selectedTicket = { ...updated };
        this.newMessage = '';
        alert('Ticket mis à jour avec succès');
      },
      error: () => {
        alert('Erreur lors de la mise à jour du ticket');
      },
    });
  }
}
