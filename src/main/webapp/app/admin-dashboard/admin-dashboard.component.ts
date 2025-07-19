import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { SidebarComponent } from '../layouts/main/sidebar.component';
import { HttpClient } from '@angular/common/http';
import { MatCardModule } from '@angular/material/card';
import { MatGridListModule } from '@angular/material/grid-list';
import { MatListModule } from '@angular/material/list';
import { MatIconModule } from '@angular/material/icon';
import { MatBadgeModule } from '@angular/material/badge';
import { MatButtonModule } from '@angular/material/button';
import { MatActionList } from '@angular/material/list';
import { AccountService } from 'app/core/auth/account.service';
import { PaiementsService } from 'app/admin/paiements/paiements.service';
import { ClientsService } from 'app/admin/clients/clients.service';

interface Ticket {
  id: number;
  type: string;
  description: string;
  createdDate?: string;
  status?: string;
}

@Component({
  selector: 'app-admin-dashboard',
  templateUrl: './admin-dashboard.component.html',
  styleUrls: ['./admin-dashboard.component.scss'],
  standalone: true,
  imports: [
    CommonModule,
    SidebarComponent,
    MatCardModule,
    MatGridListModule,
    MatListModule,
    MatIconModule,
    MatBadgeModule,
    MatButtonModule,
    MatActionList,
  ],
})
export class AdminDashboardComponent implements OnInit {
  account: any = null;

  // Dashboard résumé (propriétés supprimées)
  // ticketsCount = 0;
  // ticketsToday = 0;
  // ticketsPending = 0;
  // clientsCount = 0;
  // clientsToday = 0;
  // paiementsCount = 0;
  // paiementsToday = 0;
  recentActivities: { text: string; time: string }[] = [
    { text: 'Ticket #204 fermé par admin', time: 'il y a 1 h' },
    { text: 'Nouveau paiement en attente', time: 'il y a 30 min' },
    { text: 'Nouveau client ajouté : SARL WebExpress', time: 'il y a 3 h' },
  ];

  // Pour le tableau des tickets
  tickets: Ticket[] = [];
  loading = false;
  error: string | null = null;
  stats = { tickets: 0, open: 0, closed: 0, urgent: 0 };

  private readonly accountService = inject(AccountService);
  private readonly http = inject(HttpClient);
  private readonly paiementsService = inject(PaiementsService);
  private readonly clientsService = inject(ClientsService);

  ngOnInit(): void {
    this.accountService.identity().subscribe(account => {
      this.account = account;
    });
    this.loadTickets();
    // this.loadClients();
    // this.loadPaiements();
  }

  loadTickets(): void {
    this.loading = true;
    this.http.get<Ticket[]>('/api/tickets').subscribe({
      next: tickets => {
        this.tickets = tickets;
        // this.ticketsCount = tickets.length;
        // const today = new Date().toISOString().slice(0, 10);
        // this.ticketsToday = tickets.filter(t => t.createdDate && t.createdDate.startsWith(today)).length;
        // this.ticketsPending = tickets.filter(t => t.status === 'Pending' || t.status === 'En attente').length;
        // Stats pour les cartes existantes
        this.stats.tickets = tickets.length;
        this.stats.open = tickets.filter(t => t.status === 'Nouveau' || t.status === 'En cours').length;
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

  // loadClients(): void {
  //   this.clientsService.getAll().subscribe(clients => {
  //     this.clientsCount = clients.length;
  //     const today = new Date().toISOString().slice(0, 10);
  //     this.clientsToday = clients.filter(c => c.createdDate && c.createdDate.startsWith(today)).length;
  //   });
  // }

  // loadPaiements(): void {
  //   this.paiementsService.getPaiements().subscribe(paiements => {
  //     this.paiementsCount = paiements.length;
  //     const today = new Date().toISOString().slice(0, 10);
  //     this.paiementsToday = paiements.filter(p => p.date && p.date.startsWith(today)).length;
  //   });
  // }
}
