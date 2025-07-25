import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { HttpClient } from '@angular/common/http';
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
  selector: 'app-client-dashboard',
  templateUrl: './client-dashboard.component.html',
  styleUrls: ['./client-dashboard.component.scss'],
  standalone: true,
  imports: [CommonModule, SharedModule],
})
export class ClientDashboardComponent implements OnInit {
  tickets: Ticket[] = [];
  loading = false;
  error: string | null = null;
  stats = { tickets: 0, open: 0, closed: 0 };

  constructor(private http: HttpClient) {}

  ngOnInit(): void {
    this.fetchTickets();
  }

  fetchTickets(): void {
    this.loading = true;
    this.http.get<Ticket[]>('/api/tickets').subscribe({
      next: tickets => {
        this.tickets = tickets;
        this.stats.tickets = tickets.length;
        this.stats.open = tickets.filter(t => t.status === 'Nouveau' || t.status === 'En cours').length;
        this.stats.closed = tickets.filter(t => t.status === 'Résolu' || t.status === 'Fermé').length;
        this.loading = false;
      },
      error: () => {
        this.error = 'Erreur lors du chargement des tickets';
        this.loading = false;
      },
    });
  }
}
