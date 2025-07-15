import { Component, OnInit, signal, ViewChild, ElementRef, ChangeDetectorRef } from '@angular/core';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule, FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';

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
}

@Component({
  selector: 'app-user-tickets',
  templateUrl: './user-tickets.component.html',
  styleUrls: ['./user-tickets.component.scss'],
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, FormsModule],
})
export class UserTicketsComponent implements OnInit {
  tickets = signal<Ticket[]>([]);
  loading = signal(false);
  error = signal<string | null>(null);
  ticketForm: FormGroup;
  ticketTypes = ['Bug', 'Demande', 'Support', 'Autre'];
  showModal = signal(false);
  workflowSteps = ['Nouveau', 'En cours', 'Résolu', 'Fermé'];

  selectedTicket: Ticket | null = null;
  showDetailModal = signal(false);

  getStepIndex(status: string): number {
    return this.workflowSteps.indexOf(status);
  }

  openDetailModal(ticket: Ticket): void {
    this.selectedTicket = ticket;
    this.showDetailModal.set(true);
  }

  closeDetailModal(): void {
    this.showDetailModal.set(false);
    this.selectedTicket = null;
  }

  @ViewChild('typeInput') typeInput?: ElementRef;

  constructor(
    private http: HttpClient,
    private fb: FormBuilder,
    private cdr: ChangeDetectorRef,
  ) {
    this.ticketForm = this.fb.group({
      type: ['', Validators.required],
      description: ['', Validators.required],
      backofficeUrl: [''],
      backofficeLogin: [''],
      backofficePassword: [''],
      hostingUrl: [''],
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
  }

  submit(): void {
    if (this.ticketForm.invalid) return;
    this.loading.set(true);
    this.http.post<Ticket>('/api/tickets', this.ticketForm.value).subscribe({
      next: ticket => {
        this.tickets.set([ticket, ...this.tickets()]);
        this.closeModal();
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Erreur lors de la création du ticket');
        this.loading.set(false);
      },
    });
  }
}
