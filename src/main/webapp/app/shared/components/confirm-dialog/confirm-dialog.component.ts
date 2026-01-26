import { Component, inject } from '@angular/core';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { CommonModule } from '@angular/common';
import SharedModule from '../../shared.module';

@Component({
  selector: 'jhi-confirm-dialog',
  standalone: true,
  imports: [CommonModule, SharedModule],
  template: `
    <div class="modal-header">
      <h4 class="modal-title">{{ title }}</h4>
      <button type="button" class="btn-close" aria-label="Close" (click)="cancel()"></button>
    </div>
    <div class="modal-body">
      <p>{{ message }}</p>
    </div>
    <div class="modal-footer">
      <button type="button" class="btn btn-secondary" (click)="cancel()">
        <span>Annuler</span>
      </button>
      <button type="button" class="btn btn-danger" (click)="confirm()">
        <span>Confirmer</span>
      </button>
    </div>
  `,
})
export class ConfirmDialogComponent {
  title = 'Confirmation';
  message = 'Êtes-vous sûr de vouloir continuer ?';

  private readonly activeModal = inject(NgbActiveModal);

  cancel(): void {
    this.activeModal.dismiss();
  }

  confirm(): void {
    this.activeModal.close(true);
  }
}
