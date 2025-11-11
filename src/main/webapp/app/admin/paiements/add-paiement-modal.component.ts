import { Component, EventEmitter, Output, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { IPaiement, NewPaiement } from './paiement.model';
import { PaiementsService } from './paiements.service';
import { ClientsService, AppUser } from '../clients/clients.service';
import SharedModule from '../../shared/shared.module';
import { debounceTime, distinctUntilChanged, switchMap } from 'rxjs/operators';
import { Observable, of } from 'rxjs';

@Component({
  selector: 'jhi-add-paiement-modal',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule, SharedModule],
  template: `
    <div class="modal-header">
      <h4 class="modal-title">{{ 'paiementsAdmin.form.title' | translate }}</h4>
      <button type="button" class="btn-close" aria-label="Close" (click)="activeModal.dismiss()"></button>
    </div>
    <div class="modal-body">
      <form [formGroup]="paiementForm" (ngSubmit)="onSubmit()">
        <div class="row">
          <div class="col-md-6">
            <div class="form-group">
              <label for="user" class="form-label">{{ 'paiementsAdmin.form.client' | translate }} *</label>
              <div class="client-input-container">
                <input
                  type="text"
                  id="user"
                  formControlName="user"
                  class="form-control"
                  placeholder="Nom du client"
                  [class.is-invalid]="paiementForm.get('user')?.invalid && paiementForm.get('user')?.touched"
                  (focus)="onClientInputFocus()"
                  (blur)="onClientInputBlur()"
                  (keydown)="onClientInputKeydown($event)"
                  autocomplete="off"
                />
                <div class="client-suggestions" *ngIf="showClientSuggestions && filteredClients.length > 0">
                  <div
                    class="suggestion-item"
                    *ngFor="let client of filteredClients; let i = index"
                    [class.selected]="i === selectedIndex"
                    (mousedown)="onSuggestionClick($event, client)"
                    (touchstart)="onSuggestionClick($event, client)"
                  >
                    <div class="client-name">{{ client.firstName }} {{ client.lastName }}</div>
                    <div class="client-email">{{ client.email }}</div>
                  </div>
                </div>
              </div>
              <div class="invalid-feedback" *ngIf="paiementForm.get('user')?.invalid && paiementForm.get('user')?.touched">
                {{ 'paiementsAdmin.form.clientRequired' | translate }}
              </div>
            </div>
          </div>
          <div class="col-md-6">
            <div class="form-group">
              <label for="amount" class="form-label">{{ 'paiementsAdmin.form.amount' | translate }} *</label>
              <input
                type="number"
                id="amount"
                formControlName="amount"
                class="form-control"
                placeholder="0.00"
                step="0.01"
                min="0"
                [class.is-invalid]="paiementForm.get('amount')?.invalid && paiementForm.get('amount')?.touched"
              />
              <div class="invalid-feedback" *ngIf="paiementForm.get('amount')?.invalid && paiementForm.get('amount')?.touched">
                {{ 'paiementsAdmin.form.amountRequired' | translate }}
              </div>
            </div>
          </div>
        </div>

        <div class="row">
          <div class="col-md-6">
            <div class="form-group">
              <label for="currency" class="form-label">{{ 'paiementsAdmin.form.currency' | translate }}</label>
              <select id="currency" formControlName="currency" class="form-select">
                <option value="MAD">MAD (د.م)</option>
                <option value="EUR">EUR (€)</option>
                <option value="USD">USD ($)</option>
                <option value="GBP">GBP (£)</option>
              </select>
            </div>
          </div>
          <div class="col-md-6">
            <div class="form-group">
              <label for="status" class="form-label">{{ 'paiementsAdmin.form.status' | translate }}</label>
              <select id="status" formControlName="status" class="form-select">
                <option value="PENDING">En attente</option>
                <option value="COMPLETED">Complété</option>
                <option value="FAILED">Échoué</option>
              </select>
            </div>
          </div>
        </div>

        <div class="form-group">
          <label for="description" class="form-label">{{ 'paiementsAdmin.form.description' | translate }}</label>
          <textarea
            id="description"
            formControlName="description"
            class="form-control"
            rows="3"
            placeholder="{{ 'paiementsAdmin.form.descriptionPlaceholder' | translate }}"
          ></textarea>
        </div>

        <div class="row">
          <div class="col-md-6">
            <div class="form-group">
              <label for="cmiOrderId" class="form-label">{{ 'paiementsAdmin.form.cmiOrderId' | translate }}</label>
              <input type="text" id="cmiOrderId" formControlName="cmiOrderId" class="form-control" placeholder="ID de commande" />
            </div>
          </div>
          <div class="col-md-6">
            <div class="form-group">
              <label for="cmiTransactionId" class="form-label">{{ 'paiementsAdmin.form.cmiTransactionId' | translate }}</label>
              <input
                type="text"
                id="cmiTransactionId"
                formControlName="cmiTransactionId"
                class="form-control"
                placeholder="ID de transaction"
              />
            </div>
          </div>
        </div>

        <div class="row">
          <div class="col-md-6">
            <div class="form-group">
              <label for="cmiPaymentMethod" class="form-label">{{ 'paiementsAdmin.form.paymentMethod' | translate }}</label>
              <select id="cmiPaymentMethod" formControlName="cmiPaymentMethod" class="form-select">
                <option value="">{{ 'paiementsAdmin.form.selectPaymentMethod' | translate }}</option>
                <option value="CARD">Carte bancaire</option>
                <option value="BANK_TRANSFER">Virement bancaire</option>
                <option value="CASH">Espèces</option>
                <option value="CHECK">Chèque</option>
              </select>
            </div>
          </div>
          <div class="col-md-6">
            <div class="form-group">
              <label for="cmiCardType" class="form-label">{{ 'paiementsAdmin.form.cardType' | translate }}</label>
              <select id="cmiCardType" formControlName="cmiCardType" class="form-select">
                <option value="">{{ 'paiementsAdmin.form.selectCardType' | translate }}</option>
                <option value="VISA">Visa</option>
                <option value="MASTERCARD">Mastercard</option>
                <option value="AMEX">American Express</option>
                <option value="DISCOVER">Discover</option>
              </select>
            </div>
          </div>
        </div>

        <div class="form-group">
          <label for="cmiCardHolder" class="form-label">{{ 'paiementsAdmin.form.cardHolder' | translate }}</label>
          <input type="text" id="cmiCardHolder" formControlName="cmiCardHolder" class="form-control" placeholder="Nom du titulaire" />
        </div>

        <div class="modal-footer">
          <button type="button" class="btn btn-secondary" (click)="activeModal.dismiss()">
            {{ 'paiementsAdmin.form.cancel' | translate }}
          </button>
          <button type="submit" class="btn btn-primary" [disabled]="paiementForm.invalid || isSubmitting">
            <span *ngIf="isSubmitting" class="spinner-border spinner-border-sm me-2"></span>
            {{ isSubmitting ? ('paiementsAdmin.form.submitting' | translate) : ('paiementsAdmin.form.submit' | translate) }}
          </button>
        </div>
      </form>
    </div>
  `,
  styles: [
    `
      .modal-header {
        background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
        color: white;
        border-bottom: none;
      }

      .modal-title {
        font-weight: 600;
      }

      .btn-close {
        filter: invert(1);
      }

      .form-label {
        font-weight: 600;
        color: #495057;
        margin-bottom: 0.5rem;
      }

      .form-control,
      .form-select {
        border-radius: 8px;
        border: 1px solid #dee2e6;
        padding: 0.75rem;
        transition: all 0.3s ease;
      }

      .form-control:focus,
      .form-select:focus {
        border-color: #667eea;
        box-shadow: 0 0 0 0.2rem rgba(102, 126, 234, 0.25);
      }

      .form-group {
        margin-bottom: 1.5rem;
      }

      .modal-footer {
        border-top: 1px solid #dee2e6;
        padding: 1rem;
      }

      .btn-primary {
        background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
        border: none;
        border-radius: 8px;
        padding: 0.75rem 1.5rem;
        font-weight: 600;
      }

      .btn-primary:hover {
        transform: translateY(-1px);
        box-shadow: 0 4px 12px rgba(102, 126, 234, 0.3);
      }

      .btn-secondary {
        border-radius: 8px;
        padding: 0.75rem 1.5rem;
        font-weight: 600;
      }

      .client-input-container {
        position: relative;
      }

      .client-suggestions {
        position: absolute;
        top: 100%;
        left: 0;
        right: 0;
        background: white;
        border: 1px solid #dee2e6;
        border-radius: 8px;
        box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
        z-index: 1000;
        max-height: 200px;
        overflow-y: auto;
      }

      .suggestion-item {
        padding: 0.75rem;
        cursor: pointer;
        border-bottom: 1px solid #f8f9fa;
        transition: all 0.2s ease;
        user-select: none;
        position: relative;
      }

      .suggestion-item:hover {
        background-color: #e9ecef;
        transform: translateY(-1px);
      }

      .suggestion-item:active {
        background-color: #dee2e6;
        transform: translateY(0);
      }

      .suggestion-item:last-child {
        border-bottom: none;
      }

      .suggestion-item::before {
        content: '';
        position: absolute;
        left: 0;
        top: 0;
        bottom: 0;
        width: 3px;
        background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
        opacity: 0;
        transition: opacity 0.2s ease;
      }

      .suggestion-item:hover::before {
        opacity: 1;
      }

      .suggestion-item.selected {
        background-color: #667eea;
        color: white;
      }

      .suggestion-item.selected .client-name,
      .suggestion-item.selected .client-email {
        color: white;
      }

      .suggestion-item.selected::before {
        opacity: 1;
        background: white;
      }

      .client-name {
        font-weight: 600;
        color: #495057;
        margin-bottom: 0.25rem;
      }

      .client-email {
        font-size: 0.875rem;
        color: #6c757d;
      }
    `,
  ],
})
export class AddPaiementModalComponent implements OnInit {
  @Output() paiementAdded = new EventEmitter<IPaiement>();

  paiementForm: FormGroup;
  isSubmitting = false;
  clients: AppUser[] = [];
  filteredClients: AppUser[] = [];
  showClientSuggestions = false;
  selectedIndex = -1;

  constructor(
    public activeModal: NgbActiveModal,
    private formBuilder: FormBuilder,
    private paiementsService: PaiementsService,
    private clientsService: ClientsService,
  ) {
    this.paiementForm = this.formBuilder.group({
      user: ['', [Validators.required, Validators.minLength(2)]],
      amount: ['', [Validators.required, Validators.min(0.01)]],
      currency: ['MAD'],
      status: ['PENDING'],
      description: [''],
      cmiOrderId: [''],
      cmiTransactionId: [''],
      cmiPaymentMethod: [''],
      cmiCardType: [''],
      cmiCardHolder: [''],
    });
  }

  ngOnInit(): void {
    this.loadClients();
    this.setupClientSearch();
  }

  onSubmit(): void {
    if (this.paiementForm.valid) {
      this.isSubmitting = true;

      const newPaiement: NewPaiement = {
        id: null,
        ...this.paiementForm.value,
        date: new Date().toISOString().split('T')[0], // Format YYYY-MM-DD
      };

      this.paiementsService.create(newPaiement).subscribe({
        next: response => {
          this.isSubmitting = false;
          this.paiementAdded.emit(response.body!);
          this.activeModal.close(response.body);
        },
        error: error => {
          this.isSubmitting = false;
          console.error("Erreur lors de l'ajout du paiement:", error);

          // Afficher plus de détails sur l'erreur
          if (error.status === 400) {
            alert('Données invalides. Vérifiez que tous les champs requis sont remplis correctement.');
          } else if (error.status === 401) {
            alert('Non autorisé. Veuillez vous reconnecter.');
          } else if (error.status === 403) {
            alert("Accès refusé. Vous n'avez pas les permissions nécessaires.");
          } else {
            alert("Erreur lors de l'ajout du paiement: " + (error.message || 'Erreur inconnue'));
          }
        },
      });
    } else {
      this.markFormGroupTouched();
    }
  }

  private markFormGroupTouched(): void {
    Object.keys(this.paiementForm.controls).forEach(key => {
      const control = this.paiementForm.get(key);
      control?.markAsTouched();
    });
  }

  private loadClients(): void {
    this.clientsService.getAll().subscribe({
      next: response => {
        this.clients = response.body || [];
      },
      error(error) {
        console.error('Erreur lors du chargement des clients:', error);
      },
    });
  }

  private setupClientSearch(): void {
    this.paiementForm
      .get('user')
      ?.valueChanges.pipe(debounceTime(300), distinctUntilChanged())
      .subscribe(value => {
        if (value && value.length >= 2) {
          this.filterClients(value);
          this.showClientSuggestions = true;
          this.selectedIndex = -1;
        } else {
          this.showClientSuggestions = false;
          this.filteredClients = [];
          this.selectedIndex = -1;
        }
      });
  }

  private filterClients(searchTerm: string): void {
    const term = searchTerm.toLowerCase();
    this.filteredClients = this.clients.filter(
      client =>
        client.firstName?.toLowerCase().includes(term) ||
        client.lastName?.toLowerCase().includes(term) ||
        client.email?.toLowerCase().includes(term),
    );
  }

  selectClient(client: AppUser): void {
    const clientName = `${client.firstName} ${client.lastName}`.trim();
    this.paiementForm.patchValue({ user: clientName });
    this.showClientSuggestions = false;
    console.warn('Client sélectionné:', clientName);
  }

  onClientInputFocus(): void {
    if (this.paiementForm.get('user')?.value && this.paiementForm.get('user')?.value.length >= 2) {
      this.showClientSuggestions = true;
    }
  }

  onClientInputBlur(): void {
    // Délai pour permettre le clic sur une suggestion
    setTimeout(() => {
      this.showClientSuggestions = false;
    }, 300);
  }

  onSuggestionClick(event: Event, client: AppUser): void {
    event.preventDefault();
    event.stopPropagation();
    this.selectClient(client);
  }

  onClientInputKeydown(event: KeyboardEvent): void {
    if (!this.showClientSuggestions || this.filteredClients.length === 0) {
      return;
    }

    switch (event.key) {
      case 'ArrowDown':
        event.preventDefault();
        this.selectedIndex = Math.min(this.selectedIndex + 1, this.filteredClients.length - 1);
        break;
      case 'ArrowUp':
        event.preventDefault();
        this.selectedIndex = Math.max(this.selectedIndex - 1, -1);
        break;
      case 'Enter':
        event.preventDefault();
        if (this.selectedIndex >= 0 && this.selectedIndex < this.filteredClients.length) {
          this.selectClient(this.filteredClients[this.selectedIndex]);
        }
        break;
      case 'Escape':
        this.showClientSuggestions = false;
        this.selectedIndex = -1;
        break;
    }
  }
}
