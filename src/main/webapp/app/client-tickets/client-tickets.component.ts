import { Component, OnInit, signal, ViewChild, ElementRef, ChangeDetectorRef, computed, inject, OnDestroy } from '@angular/core';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule, FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { HttpClient, HttpEvent, HttpEventType } from '@angular/common/http';
import { ActivatedRoute, Router } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';
import { Subject, takeUntil } from 'rxjs';
import SharedModule from '../shared/shared.module';
import { AppParametersService } from 'app/core/services/app-parameters.service';
import { AppParameter } from 'app/admin/parameters/parameters.model';
import { NotificationBadgeComponent } from '../shared/components/notification-badge/notification-badge.component';

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
  selector: 'jhi-client-tickets',
  templateUrl: './client-tickets.component.html',
  styleUrls: ['./client-tickets.component.scss'],
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, FormsModule, SharedModule, NotificationBadgeComponent],
})
export class ClientTicketsComponent implements OnInit, OnDestroy {
  private readonly destroy$ = new Subject<void>();
  private readonly appParametersService = inject(AppParametersService);

  tickets = signal<Ticket[]>([]);
  loading = signal(false);
  error = signal<string | null>(null);
  success = signal<string | null>(null);
  ticketForm: FormGroup;
  ticketTypes = ['Bug', 'Demande', 'Support', 'Autre'];
  showModal = signal(false);
  workflowSteps = ['Nouveau', 'En cours', 'Résolu', 'En attente de paiement', 'Paiement validé', 'Fermé'];
  statusFilter = signal<string>('');

  // Paramètres de l'application
  ticketStatuses: AppParameter[] = [];
  ticketTypesParams: AppParameter[] = [];
  ticketPriorities: AppParameter[] = [];

  // Valeurs par défaut
  defaultStatus = 'En cours';
  defaultType = 'Support';
  defaultPriority = 'Normal';
  maxTicketsPerUser = 10;
  supportEmail = 'support@devtechly.com';
  companyName = 'devtechly';

  // États de chargement
  loadingParameters = signal(false);

  // Image upload signals
  selectedImagePreview = signal<string | null>(null);
  uploadProgress = signal<number>(0);
  selectedFile: File | null = null;

  // Image modal signals
  showImageModal = signal<boolean>(false);
  selectedImageUrl = signal<string | null>(null);

  // Gestion des devis et validations
  showDevisModal = signal<boolean>(false);
  showPaymentValidationModal = signal<boolean>(false);
  selectedTicketForDevis: Ticket | null = null;
  selectedTicketForPayment: Ticket | null = null;
  devisAmount = signal<number>(0);
  devisDescription = signal<string>('');

  // Computed properties for filtered tickets and statistics
  filteredTickets = computed(() => {
    const filter = this.statusFilter();
    if (!filter) return this.tickets();
    return this.tickets().filter(ticket => ticket.status === filter);
  });

  @ViewChild('typeInput') typeInput?: ElementRef;

  constructor(
    private http: HttpClient,
    private fb: FormBuilder,
    private cdr: ChangeDetectorRef,
    private translateService: TranslateService,
    private router: Router,
    private route: ActivatedRoute,
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
    this.loadApplicationParameters();
    this.route.queryParams.pipe(takeUntil(this.destroy$)).subscribe(params => {
      if (params['create'] === 'true') {
        this.router.navigate([], { relativeTo: this.route, queryParams: {}, queryParamsHandling: '' });
        setTimeout(() => this.openModal(), 300);
      }
    });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

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

  openTicketDetail(ticket: Ticket): void {
    this.router.navigate(['/client-tickets', ticket.id]);
  }

  onImageSelected(event: any): void {
    const file = event.target.files[0];
    if (file) {
      // Validate file type
      if (!file.type.startsWith('image/')) {
        this.error.set(this.translateService.instant('ticketsClient.messages.invalidImage'));
        return;
      }

      // Validate file size (5MB max)
      if (file.size > 5 * 1024 * 1024) {
        this.error.set(this.translateService.instant('ticketsClient.messages.imageTooLarge'));
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

  openDevisModal(ticket: Ticket): void {
    this.selectedTicketForDevis = ticket;
    this.showDevisModal.set(true);
  }

  closeDevisModal(): void {
    this.showDevisModal.set(false);
    this.selectedTicketForDevis = null;
    this.devisAmount.set(0);
    this.devisDescription.set('');
  }

  submitDevis(): void {
    if (!this.selectedTicketForDevis || this.devisAmount() <= 0) {
      this.showErrorMessage(this.translateService.instant('ticketsClient.messages.devisInvalid'));
      return;
    }

    const devisData = {
      ticketId: this.selectedTicketForDevis.id,
      amount: this.devisAmount(),
      description: this.devisDescription(),
      status: 'PENDING_VALIDATION',
    };

    this.http.post('/api/tickets/devis', devisData).subscribe({
      next: () => {
        this.showSuccessMessage(this.translateService.instant('ticketsClient.messages.devisSuccess'));
        this.closeDevisModal();
        this.fetchTickets();
      },
      error: () => {
        this.showErrorMessage(this.translateService.instant('ticketsClient.messages.devisError'));
      },
    });
  }

  // validateDevis method removed - no longer needed in simplified workflow

  openPaymentValidationModal(ticket: Ticket): void {
    this.selectedTicketForPayment = ticket;
    this.showPaymentValidationModal.set(true);
  }

  closePaymentValidationModal(): void {
    this.showPaymentValidationModal.set(false);
    this.selectedTicketForPayment = null;
  }

  validatePayment(ticketId: number): void {
    this.http.put(`/api/tickets/${ticketId}/validate-payment`, {}).subscribe({
      next: () => {
        this.showSuccessMessage(this.translateService.instant('ticketsClient.messages.paymentSuccess'));
        this.fetchTickets();
      },
      error: () => {
        this.showErrorMessage(this.translateService.instant('ticketsClient.messages.paymentError'));
      },
    });
  }

  canValidateDevis(ticket: Ticket): boolean {
    return ticket.status === 'Nouveau';
  }

  canValidatePayment(ticket: Ticket): boolean {
    return ticket.status === 'En attente de paiement';
  }

  canCloseTicket(ticket: Ticket): boolean {
    return ticket.status === 'Paiement validé';
  }

  closeTicket(ticketId: number): void {
    this.http.put(`/api/tickets/${ticketId}/close`, {}).subscribe({
      next: () => {
        this.showSuccessMessage(this.translateService.instant('ticketsClient.messages.closeSuccess'));
        this.fetchTickets();
      },
      error: () => {
        this.showErrorMessage(this.translateService.instant('ticketsClient.messages.closeError'));
      },
    });
  }

  getTicketTypeTranslation(type: string | undefined): string {
    if (!type) return '';

    const translationKey = `ticketsClient.types.${type.toLowerCase()}`;
    const translated = this.translateService.instant(translationKey);
    return translated === translationKey ? type : translated;
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

  getStatusLabel(statusKey: string): string {
    const status = this.ticketStatuses.find(s => s.key === statusKey);
    return status?.value ?? statusKey;
  }

  getTypeLabel(typeKey: string): string {
    const type = this.ticketTypesParams.find(t => t.key === typeKey);
    return type?.value ?? typeKey;
  }

  getPriorityLabel(priorityKey: string): string {
    const priority = this.ticketPriorities.find(p => p.key === priorityKey);
    return priority?.value ?? priorityKey;
  }

  getAdminMessagesLabel(count: number): string {
    const messageCount = count || 0;
    if (messageCount <= 1) {
      return this.translateService.instant('ticketsClient.adminMessages.singular', { count: messageCount });
    }
    return this.translateService.instant('ticketsClient.adminMessages.plural', { count: messageCount });
  }

  getStatusColor(statusKey: string): string {
    switch (statusKey) {
      case 'open':
        return 'warning';
      case 'resolved':
        return 'success';
      case 'closed':
        return 'secondary';
      case 'urgent':
        return 'danger';
      default:
        return 'info';
    }
  }

  getPriorityColor(priorityKey: string): string {
    switch (priorityKey) {
      case 'low':
        return 'info';
      case 'normal':
        return 'primary';
      case 'high':
        return 'warning';
      case 'critical':
        return 'danger';
      default:
        return 'primary';
    }
  }

  canCreateNewTicket(): boolean {
    return this.tickets().length < this.maxTicketsPerUser;
  }

  getMaxTicketsErrorMessage(): string {
    return this.translateService.instant('tickets.maxTicketsReached', {
      max: this.maxTicketsPerUser,
      supportEmail: this.supportEmail,
    }) as string;
  }

  refreshParameters(): void {
    this.appParametersService.refreshParameters();
    this.loadApplicationParameters();
  }

  fetchTickets(): void {
    this.loading.set(true);
    this.http.get<Ticket[]>('/api/tickets').subscribe({
      next: tickets => {
        this.tickets.set(tickets);
        this.loading.set(false);
      },
      error: () => {
        this.error.set(this.translateService.instant('ticketsClient.messages.loadTicketsError'));
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
    this.error.set(null);

    const formData = new FormData();
    formData.append('type', this.ticketForm.value.type);
    formData.append('description', this.ticketForm.value.description);
    formData.append('backofficeUrl', this.ticketForm.value.backofficeUrl ?? '');
    formData.append('backofficeLogin', this.ticketForm.value.backofficeLogin ?? '');
    formData.append('backofficePassword', this.ticketForm.value.backofficePassword ?? '');
    formData.append('hostingUrl', this.ticketForm.value.hostingUrl ?? '');

    if (this.selectedFile) {
      formData.append('image', this.selectedFile);
    }

    this.http
      .post<Ticket>('/api/tickets', formData, {
        reportProgress: true,
        observe: 'events',
      })
      .subscribe({
        next: (event: HttpEvent<any>) => {
          if (event.type === HttpEventType.UploadProgress) {
            const progress = Math.round((100 * event.loaded) / (event.total ?? 1));
            this.uploadProgress.set(progress);
          } else if (event.type === HttpEventType.Response) {
            const ticket = event.body;
            this.tickets.set([ticket, ...this.tickets()]);
            this.closeModal();
            this.loading.set(false);
            this.uploadProgress.set(100);
            this.showSuccessMessage(this.translateService.instant('ticketsClient.messages.createSuccess'));
          }
        },
        error: () => {
          this.error.set(this.translateService.instant('ticketsClient.messages.createError'));
          this.loading.set(false);
          this.uploadProgress.set(0);
          this.clearErrorAfterDelay();
        },
      });
  }

  private loadApplicationParameters(): void {
    this.loadingParameters.set(true);

    this.appParametersService
      .getTicketStatuses()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: statuses => {
          this.ticketStatuses = statuses;
        },
        error: (error: any) => {
          const message = this.translateService.instant('ticketsClient.messages.loadStatusesError', {
            error: error.message || this.translateService.instant('ticketsClient.messages.unknownError'),
          });
          this.error.set(message);
        },
      });

    this.appParametersService
      .getTicketTypes()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: types => {
          this.ticketTypesParams = types;
        },
        error: (error: any) => {
          const message = this.translateService.instant('ticketsClient.messages.loadTypesError', {
            error: error.message || this.translateService.instant('ticketsClient.messages.unknownError'),
          });
          this.error.set(message);
        },
      });

    this.appParametersService
      .getTicketPriorities()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: priorities => {
          this.ticketPriorities = priorities;
        },
        error: (error: any) => {
          const message = this.translateService.instant('ticketsClient.messages.loadPrioritiesError', {
            error: error.message || this.translateService.instant('ticketsClient.messages.unknownError'),
          });
          this.error.set(message);
        },
      });

    this.appParametersService
      .getDefaultTicketStatus()
      .pipe(takeUntil(this.destroy$))
      .subscribe(status => (this.defaultStatus = status));

    this.appParametersService
      .getDefaultTicketType()
      .pipe(takeUntil(this.destroy$))
      .subscribe(type => (this.defaultType = type));

    this.appParametersService
      .getDefaultTicketPriority()
      .pipe(takeUntil(this.destroy$))
      .subscribe(priority => (this.defaultPriority = priority));

    this.appParametersService
      .getParameterValue('max_tickets_per_user')
      .pipe(takeUntil(this.destroy$))
      .subscribe(value => {
        if (value) {
          this.maxTicketsPerUser = parseInt(value, 10);
        }
      });

    this.appParametersService
      .getParameterValue('support_email')
      .pipe(takeUntil(this.destroy$))
      .subscribe(value => {
        if (value) {
          this.supportEmail = value;
        }
      });

    this.appParametersService
      .getParameterValue('company_name')
      .pipe(takeUntil(this.destroy$))
      .subscribe(value => {
        if (value) {
          this.companyName = value;
        }
      });

    this.loadingParameters.set(false);
  }

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

  private clearErrorAfterDelay(delay = 5000): void {
    setTimeout(() => {
      this.error.set(null);
    }, delay);
  }
}
