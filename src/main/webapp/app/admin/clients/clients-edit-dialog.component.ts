import { Component, Input } from '@angular/core';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { AppUser } from './clients.service';

@Component({
  selector: 'app-clients-edit-dialog',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="modal-header">
      <h5 class="modal-title">Modifier le client</h5>
      <button type="button" class="btn-close" aria-label="Close" (click)="activeModal.dismiss()"></button>
    </div>
    <form (ngSubmit)="submit()">
      <div class="modal-body row g-2">
        <div class="col-md-6">
          <input class="form-control" [(ngModel)]="client.firstName" name="firstName" required placeholder="Prénom" />
        </div>
        <div class="col-md-6">
          <input class="form-control" [(ngModel)]="client.lastName" name="lastName" required placeholder="Nom" />
        </div>
        <div class="col-md-6">
          <input class="form-control" [(ngModel)]="client.email" name="email" required placeholder="Email" type="email" />
        </div>
        <div class="col-md-6">
          <input class="form-control" [(ngModel)]="client.phone" name="phone" required placeholder="Téléphone" />
        </div>
        <div class="col-md-6">
          <input class="form-control" [(ngModel)]="client.password" name="password" required placeholder="Mot de passe" type="password" />
        </div>
      </div>
      <div class="modal-footer">
        <button type="submit" class="btn btn-success">Enregistrer</button>
        <button type="button" class="btn btn-outline-secondary" (click)="activeModal.dismiss()">Annuler</button>
      </div>
    </form>
  `,
})
export class ClientsEditDialogComponent {
  @Input() client!: AppUser;
  constructor(public activeModal: NgbActiveModal) {}

  submit(): void {
    if (!this.client.firstName || !this.client.lastName || !this.client.email || !this.client.phone || !this.client.password) return;
    this.activeModal.close(this.client);
  }
}
