import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { SidebarComponent } from '../layouts/main/sidebar.component';
import { HttpClient } from '@angular/common/http';

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
  author?: string;
}

@Component({
  selector: 'app-admin-dashboard',
  templateUrl: './admin-dashboard.component.html',
  styleUrls: ['./admin-dashboard.component.scss'],
  standalone: true,
  imports: [CommonModule, SidebarComponent],
})
export class AdminDashboardComponent implements OnInit {
  tickets: Ticket[] = [];
  loading = false;
  error: string | null = null;
  stats = { tickets: 0, open: 0, inProgress: 0, closed: 0, urgent: 0 };

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
        this.stats.inProgress = tickets.filter(t => t.status === 'En cours').length;
        this.stats.closed = tickets.filter(t => t.status === 'Résolu' || t.status === 'Fermé').length;
        this.stats.urgent = tickets.filter(t => t.status === 'Urgent').length;
        this.loading = false;
      },
      error: () => {
        this.error = 'Erreur lors du chargement des tickets';
        this.loading = false;
      },
    });
  }
}
