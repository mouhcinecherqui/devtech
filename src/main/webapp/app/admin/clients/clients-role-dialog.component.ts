import { Component, Input, Output, EventEmitter, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { AppUser } from './clients.service';

@Component({
  selector: 'jhi-clients-role-dialog',
  template: `
    <div class="modal-header">
      <h4 class="modal-title">Modifier le rôle de l'utilisateur</h4>
      <button type="button" class="btn-close" (click)="dismiss()" aria-label="Close">
        <span aria-hidden="true">&times;</span>
      </button>
    </div>

    <div class="modal-body">
      <div class="user-info">
        <h5>{{ client?.firstName }} {{ client?.lastName }}</h5>
        <p class="text-muted">{{ client?.email }}</p>
      </div>

      <div class="form-group">
        <label for="roleSelect">Rôle actuel :</label>
        <select id="roleSelect" class="form-control" [(ngModel)]="selectedRole" #roleSelect="ngModel" required>
          <option value="">Sélectionner un rôle</option>
          <option value="CLIENT">Client</option>
          <option value="MANAGER">Manager</option>
          <option value="ADMIN">Administrateur</option>
        </select>
      </div>

      <div class="role-description" *ngIf="selectedRole">
        <h6>Description du rôle :</h6>
        <p class="role-desc" [ngClass]="'role-' + selectedRole.toLowerCase()">
          {{ getRoleDescription(selectedRole) }}
        </p>
      </div>
    </div>

    <div class="modal-footer">
      <button type="button" class="btn btn-secondary" (click)="dismiss()">Annuler</button>
      <button type="button" class="btn btn-primary" [disabled]="!selectedRole || selectedRole === client?.type" (click)="save()">
        Sauvegarder
      </button>
    </div>
  `,
  styles: [
    `
      .user-info {
        margin-bottom: 1.5rem;
        padding: 1rem;
        background-color: #f8f9fa;
        border-radius: 0.375rem;
      }

      .user-info h5 {
        margin: 0 0 0.25rem 0;
        color: #212529;
      }

      .user-info p {
        margin: 0;
        font-size: 0.875rem;
      }

      .form-group {
        margin-bottom: 1.5rem;
      }

      .form-group label {
        display: block;
        margin-bottom: 0.5rem;
        font-weight: 500;
        color: #495057;
      }

      .form-control {
        width: 100%;
        padding: 0.5rem 0.75rem;
        border: 1px solid #ced4da;
        border-radius: 0.375rem;
        font-size: 1rem;
      }

      .form-control:focus {
        border-color: #80bdff;
        outline: 0;
        box-shadow: 0 0 0 0.2rem rgba(0, 123, 255, 0.25);
      }

      .role-description {
        margin-top: 1rem;
        padding: 1rem;
        border-radius: 0.375rem;
        border: 1px solid #dee2e6;
      }

      .role-description h6 {
        margin: 0 0 0.5rem 0;
        font-weight: 600;
      }

      .role-desc {
        margin: 0;
        font-size: 0.875rem;
        line-height: 1.4;
      }

      .role-client {
        background-color: #e3f2fd;
        color: #1565c0;
      }

      .role-manager {
        background-color: #fff3e0;
        color: #ef6c00;
      }

      .role-admin {
        background-color: #fce4ec;
        color: #c2185b;
      }

      .modal-footer {
        display: flex;
        justify-content: flex-end;
        gap: 0.5rem;
      }

      .btn {
        padding: 0.5rem 1rem;
        border: none;
        border-radius: 0.375rem;
        font-size: 0.875rem;
        font-weight: 500;
        cursor: pointer;
        transition: all 0.15s ease-in-out;
      }

      .btn-secondary {
        background-color: #6c757d;
        color: white;
      }

      .btn-secondary:hover {
        background-color: #5a6268;
      }

      .btn-primary {
        background-color: #007bff;
        color: white;
      }

      .btn-primary:hover:not(:disabled) {
        background-color: #0056b3;
      }

      .btn-primary:disabled {
        background-color: #6c757d;
        cursor: not-allowed;
      }

      .btn-close {
        background: none;
        border: none;
        font-size: 1.5rem;
        cursor: pointer;
        color: #6c757d;
      }

      .btn-close:hover {
        color: #495057;
      }
    `,
  ],
  standalone: true,
  imports: [CommonModule, FormsModule],
})
export class ClientsRoleDialogComponent {
  @Input() client: AppUser | null = null;
  @Output() roleChanged = new EventEmitter<{ client: AppUser; newRole: string }>();

  selectedRole: string = '';
  private readonly activeModal = inject(NgbActiveModal);

  ngOnInit(): void {
    if (this.client?.type) {
      this.selectedRole = this.client.type;
    }
  }

  getRoleDescription(role: string): string {
    switch (role) {
      case 'CLIENT':
        return 'Peut créer et gérer ses propres tickets, accéder à ses informations personnelles.';
      case 'MANAGER':
        return 'Peut gérer les tickets des clients, accéder aux statistiques et rapports.';
      case 'ADMIN':
        return 'Accès complet à toutes les fonctionnalités, gestion des utilisateurs et configuration système.';
      default:
        return 'Rôle non défini.';
    }
  }

  save(): void {
    if (this.client && this.selectedRole && this.selectedRole !== this.client.type) {
      this.roleChanged.emit({
        client: this.client,
        newRole: this.selectedRole,
      });
      this.activeModal.close();
    }
  }

  dismiss(): void {
    this.activeModal.dismiss();
  }
}
