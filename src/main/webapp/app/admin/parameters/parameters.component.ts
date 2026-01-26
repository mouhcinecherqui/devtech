import { Component, OnInit, inject, OnDestroy, ChangeDetectorRef } from '@angular/core';
import { AppParameter } from './parameters.model';
import { ParametersService } from './parameters.service';
import { AutoRefreshService } from '../../core/services/auto-refresh.service';
import { RefreshButtonComponent } from '../../shared/components/refresh-button/refresh-button.component';
import SharedModule from 'app/shared/shared.module';
import { FormsModule } from '@angular/forms';
import { AlertService } from 'app/core/util/alert.service';
import { TranslateService } from '@ngx-translate/core';
import { Subject, takeUntil } from 'rxjs';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { ConfirmDialogComponent } from '../../shared/components/confirm-dialog/confirm-dialog.component';

// Types de paramètres utilisés pour les sections
const TICKET_STATUS = 'ticket-status';
const TICKET_TYPE = 'ticket-type';
const TICKET_PRIORITY = 'ticket-priority';

@Component({
  selector: 'jhi-admin-parameters',
  templateUrl: './parameters.component.html',
  styleUrls: ['./parameters.component.scss'],
  standalone: true,
  imports: [SharedModule, FormsModule, RefreshButtonComponent],
})
export class ParametersComponent implements OnInit, OnDestroy {
  parameters: AppParameter[] = [];
  private readonly parametersService = inject(ParametersService);
  private readonly alertService = inject(AlertService);
  private readonly translateService = inject(TranslateService);
  private readonly cdr = inject(ChangeDetectorRef);
  private readonly autoRefreshService = inject(AutoRefreshService);
  private readonly modalService = inject(NgbModal);
  private readonly destroy$ = new Subject<void>();

  // États de chargement
  loading = false;
  saving = false;
  deleting = false;

  // Pour l'ajout/édition
  showForm = false;
  editMode = false;
  form: AppParameter = { key: '', value: '', type: '', description: '' };
  formType = '';
  formErrors: Record<string, string> = {};

  // Validation des types de paramètres
  readonly parameterTypes = [
    { value: TICKET_STATUS, label: 'parameters.ticketStatus.title' },
    { value: TICKET_TYPE, label: 'parameters.ticketType.title' },
    { value: TICKET_PRIORITY, label: 'parameters.ticketPriority.title' },
    { value: 'other', label: 'parameters.other.title' },
  ];

  ngOnInit(): void {
    this.loadAll();

    // Configurer l'actualisation automatique toutes les 30 secondes
    this.autoRefreshService.setRefreshInterval(30000);
    this.autoRefreshService.refreshTrigger$.pipe(takeUntil(this.destroy$)).subscribe(() => {
      this.loadAll();
    });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  loadAll(): void {
    this.loading = true;
    this.parametersService
      .getAll()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: params => {
          this.parameters = params;
          this.loading = false;
          this.cdr.detectChanges();
        },
        error: error => {
          console.error('Erreur lors du chargement des paramètres:', error);
          this.alertService.addAlert({
            type: 'danger',
            message: this.translateService.instant('parameters.errors.loadFailed'),
            timeout: 5000,
          });
          this.loading = false;
          this.cdr.detectChanges();
        },
      });
  }

  // Filtrage par type
  get ticketStatusList(): AppParameter[] {
    return this.parameters.filter(p => p.type === TICKET_STATUS);
  }

  get ticketTypeList(): AppParameter[] {
    return this.parameters.filter(p => p.type === TICKET_TYPE);
  }

  get ticketPriorityList(): AppParameter[] {
    return this.parameters.filter(p => p.type === TICKET_PRIORITY);
  }

  get otherList(): AppParameter[] {
    return this.parameters.filter(p => ![TICKET_STATUS, TICKET_TYPE, TICKET_PRIORITY].includes(p.type ?? ''));
  }

  // Validation du formulaire
  validateForm(): boolean {
    this.formErrors = {};

    if (!this.form.key?.trim()) {
      this.formErrors.key = this.translateService.instant('parameters.validation.keyRequired');
    } else if (this.form.key.length > 100) {
      this.formErrors.key = this.translateService.instant('parameters.validation.keyTooLong');
    }

    if (!this.form.value?.trim()) {
      this.formErrors.value = this.translateService.instant('parameters.validation.valueRequired');
    } else if (this.form.value.length > 500) {
      this.formErrors.value = this.translateService.instant('parameters.validation.valueTooLong');
    }

    if (this.form.description && this.form.description.length > 255) {
      this.formErrors.description = this.translateService.instant('parameters.validation.descriptionTooLong');
    }

    // Vérifier si la clé existe déjà (sauf en mode édition)
    if (!this.editMode) {
      const existingParam = this.parameters.find(p => p.key === this.form.key);
      if (existingParam) {
        this.formErrors.key = this.translateService.instant('parameters.validation.keyExists');
      }
    }

    return Object.keys(this.formErrors).length === 0;
  }

  // Ajout/édition par type
  openAddForm(type: string): void {
    this.showForm = true;
    this.editMode = false;
    this.formType = type;
    this.form = { key: '', value: '', type, description: '' };
    this.formErrors = {};
  }

  openEditForm(param: AppParameter): void {
    this.showForm = true;
    this.editMode = true;
    this.formType = param.type ?? '';
    this.form = { ...param };
    this.formErrors = {};
  }

  save(): void {
    if (!this.validateForm()) {
      return;
    }

    this.saving = true;
    this.form.type = this.formType;

    const operation = this.editMode && this.form.id ? this.parametersService.update(this.form) : this.parametersService.create(this.form);

    operation.pipe(takeUntil(this.destroy$)).subscribe({
      next: savedParam => {
        this.saving = false;
        this.showForm = false;
        this.loadAll();

        const messageKey = this.editMode ? 'parameters.messages.updated' : 'parameters.messages.created';

        this.alertService.addAlert({
          type: 'success',
          message: this.translateService.instant(messageKey, { key: savedParam.key }),
          timeout: 3000,
        });
      },
      error: error => {
        console.error('Erreur lors de la sauvegarde:', error);
        this.saving = false;

        let errorMessage = 'parameters.errors.saveFailed';
        if (error.status === 409) {
          errorMessage = 'parameters.errors.keyExists';
        } else if (error.status === 400) {
          errorMessage = 'parameters.errors.invalidData';
        }

        this.alertService.addAlert({
          type: 'danger',
          message: this.translateService.instant(errorMessage),
          timeout: 5000,
        });
      },
    });
  }

  delete(param: AppParameter): void {
    if (!param.id) {
      this.alertService.addAlert({
        type: 'danger',
        message: this.translateService.instant('parameters.errors.invalidParameter'),
        timeout: 3000,
      });
      return;
    }

    const confirmMessage = this.translateService.instant('parameters.confirm.delete', { key: param.key });
    const modalRef = this.modalService.open(ConfirmDialogComponent, { size: 'md' });
    modalRef.componentInstance.title = this.translateService.instant('entity.delete.title');
    modalRef.componentInstance.message = confirmMessage;

    modalRef.result.then(
      (confirmed: boolean) => {
        if (confirmed && param.id != null) {
          this.deleting = true;
          this.parametersService
            .delete(param.id)
            .pipe(takeUntil(this.destroy$))
            .subscribe({
              next: () => {
                this.deleting = false;
                this.loadAll();
                this.alertService.addAlert({
                  type: 'success',
                  message: this.translateService.instant('parameters.messages.deleted', { key: param.key }),
                  timeout: 3000,
                });
              },
              error: error => {
                console.error('Erreur lors de la suppression:', error);
                this.deleting = false;
                this.alertService.addAlert({
                  type: 'danger',
                  message: this.translateService.instant('parameters.errors.deleteFailed'),
                  timeout: 5000,
                });
              },
            });
        }
      },
      () => {},
    );
  }

  cancel(): void {
    this.showForm = false;
    this.formErrors = {};
  }

  // Méthodes utilitaires
  getParameterTypeLabel(type: string): string {
    const paramType = this.parameterTypes.find(pt => pt.value === type);
    return paramType ? this.translateService.instant(paramType.label) : type;
  }

  isFormValid(): boolean {
    return !!(this.form.key?.trim() && this.form.value?.trim() && !this.saving);
  }

  getFormError(field: string): string {
    return this.formErrors[field] || '';
  }

  hasFormErrors(): boolean {
    return Object.keys(this.formErrors).length > 0;
  }
}
