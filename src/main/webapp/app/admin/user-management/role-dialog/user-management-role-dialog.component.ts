import { Component, Input, Output, EventEmitter, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { User } from '../user-management.model';

@Component({
  selector: 'jhi-user-management-role-dialog',
  template: `
    <div class="modal-header">
      <h4 class="modal-title">Modifier les rôles de l'utilisateur</h4>
      <button type="button" class="btn-close" (click)="dismiss()" aria-label="Close">
        <span aria-hidden="true">&times;</span>
      </button>
    </div>

    <div class="modal-body">
      <div class="user-info">
        <h5>{{ user?.login }}</h5>
        <p class="text-muted">{{ user?.email }}</p>
      </div>

      <div class="form-group">
        <label for="rolesSelect">Rôles actuels :</label>
        <div class="roles-container">
          <div class="role-option" *ngFor="let authority of availableAuthorities">
            <label class="role-checkbox">
              <input type="checkbox" [value]="authority" [checked]="selectedRoles.includes(authority)" (change)="toggleRole(authority)" />
              <span class="role-label" [ngClass]="'role-' + authority.toLowerCase().replace('role_', '')">
                {{ getRoleDisplayName(authority) }}
              </span>
            </label>
            <p class="role-description">{{ getRoleDescription(authority) }}</p>
          </div>
        </div>
      </div>
    </div>

    <div class="modal-footer">
      <button type="button" class="btn btn-secondary" (click)="dismiss()">Annuler</button>
      <button type="button" class="btn btn-primary" [disabled]="!hasChanges()" (click)="save()">Sauvegarder</button>
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

      .roles-container {
        display: flex;
        flex-direction: column;
        gap: 1rem;
      }

      .role-option {
        padding: 1rem;
        border: 1px solid #dee2e6;
        border-radius: 0.375rem;
        background-color: #f8f9fa;
      }

      .role-checkbox {
        display: flex;
        align-items: center;
        gap: 0.5rem;
        cursor: pointer;
        margin-bottom: 0.5rem;
      }

      .role-checkbox input[type='checkbox'] {
        width: 18px;
        height: 18px;
        cursor: pointer;
      }

      .role-label {
        font-weight: 600;
        padding: 0.25rem 0.75rem;
        border-radius: 1rem;
        font-size: 0.75rem;
        text-transform: uppercase;
        letter-spacing: 0.025em;
      }

      .role-user {
        background: linear-gradient(135deg, #e3f2fd 0%, #bbdefb 100%);
        color: #1565c0;
      }

      .role-client {
        background: linear-gradient(135deg, #e8f5e8 0%, #c8e6c9 100%);
        color: #2e7d32;
      }

      .role-manager {
        background: linear-gradient(135deg, #fff3e0 0%, #ffe0b2 100%);
        color: #ef6c00;
      }

      .role-admin {
        background: linear-gradient(135deg, #fce4ec 0%, #f8bbd9 100%);
        color: #c2185b;
      }

      .role-description {
        margin: 0;
        font-size: 0.875rem;
        color: #6c757d;
        line-height: 1.4;
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
export class UserManagementRoleDialogComponent implements OnInit {
  @Input() user: User | null = null;
  @Input() availableAuthorities: string[] = [];
  @Output() rolesChanged = new EventEmitter<{ user: User; newRoles: string[] }>();

  selectedRoles: string[] = [];
  private readonly activeModal = inject(NgbActiveModal);

  ngOnInit(): void {
    if (this.user && this.user.authorities) {
      this.selectedRoles = [...this.user.authorities];
    }
  }

  getRoleDisplayName(authority: string): string {
    switch (authority) {
      case 'ROLE_USER':
        return 'Utilisateur';
      case 'ROLE_CLIENT':
        return 'Client';
      case 'ROLE_MANAGER':
        return 'Manager';
      case 'ROLE_ADMIN':
        return 'Administrateur';
      default:
        return authority.replace('ROLE_', '');
    }
  }

  getRoleDescription(authority: string): string {
    switch (authority) {
      case 'ROLE_USER':
        return 'Utilisateur de base avec accès limité aux fonctionnalités.';
      case 'ROLE_CLIENT':
        return 'Peut créer et gérer ses propres tickets, accéder à ses informations personnelles.';
      case 'ROLE_MANAGER':
        return 'Peut gérer les tickets des clients, accéder aux statistiques et rapports.';
      case 'ROLE_ADMIN':
        return 'Accès complet à toutes les fonctionnalités, gestion des utilisateurs et configuration système.';
      default:
        return 'Rôle personnalisé.';
    }
  }

  toggleRole(authority: string): void {
    const index = this.selectedRoles.indexOf(authority);
    if (index > -1) {
      this.selectedRoles.splice(index, 1);
    } else {
      this.selectedRoles.push(authority);
    }
  }

  hasChanges(): boolean {
    if (!this.user || !this.user.authorities) return false;

    const currentRoles = [...this.user.authorities].sort();
    const newRoles = [...this.selectedRoles].sort();

    return JSON.stringify(currentRoles) !== JSON.stringify(newRoles);
  }

  save(): void {
    if (this.user && this.hasChanges()) {
      this.rolesChanged.emit({
        user: this.user,
        newRoles: this.selectedRoles,
      });
      this.activeModal.close();
    }
  }

  dismiss(): void {
    this.activeModal.dismiss();
  }
}
