import { Component, OnInit, inject, ChangeDetectorRef } from '@angular/core';
import { ClientsService, AppUser } from './clients.service';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { NgbModal, NgbPaginationModule } from '@ng-bootstrap/ng-bootstrap';
import { ClientsAddDialogComponent } from './clients-add-dialog.component';
import { ClientsEditDialogComponent } from './clients-edit-dialog.component';
import { ClientsRoleDialogComponent } from './clients-role-dialog.component';
import { AutoRefreshService } from '../../core/services/auto-refresh.service';
import { RefreshButtonComponent } from '../../shared/components/refresh-button/refresh-button.component';
import ItemCountComponent from '../../shared/pagination/item-count.component';
import SharedModule from '../../shared/shared.module';

@Component({
  selector: 'jhi-admin-clients',
  templateUrl: './clients.component.html',
  styleUrls: ['./clients.component.scss'],
  standalone: true,
  imports: [CommonModule, FormsModule, SharedModule, RefreshButtonComponent, NgbPaginationModule, ItemCountComponent],
})
export class ClientsComponent implements OnInit {
  clients: AppUser[] = [];
  searchTerm = '';
  loading = false;
  page = 1;
  itemsPerPage = 10;
  totalItems = 0;
  private readonly clientsService = inject(ClientsService);
  private readonly modalService = inject(NgbModal);
  private readonly cdr = inject(ChangeDetectorRef);
  private readonly autoRefreshService = inject(AutoRefreshService);

  ngOnInit(): void {
    this.loadClients();

    // Configurer l'actualisation automatique toutes les 30 secondes
    this.autoRefreshService.setRefreshInterval(30000);
    this.autoRefreshService.refreshTrigger$.subscribe(() => {
      this.loadClients();
    });
  }

  loadClients(): void {
    this.loading = true;
    const req = {
      page: this.page - 1,
      size: this.itemsPerPage,
      sort: ['id,asc'],
    };

    this.clientsService.getAll(req).subscribe({
      next: response => {
        this.clients = response.body || [];
        this.totalItems = parseInt(response.headers.get('X-Total-Count') || '0', 10);
        this.loading = false;
        this.cdr.detectChanges();
      },
      error: error => {
        console.error('Erreur lors du chargement des clients:', error);
        this.loading = false;
        this.cdr.detectChanges();
      },
    });
  }

  openAddForm(): void {
    const modalRef = this.modalService.open(ClientsAddDialogComponent, { size: 'lg' });
    modalRef.result.then(
      (result: Partial<AppUser> & { _generatedPassword?: string }) => {
        if (result) {
          this.clientsService.create({ ...result, type: 'client' } as AppUser).subscribe(() => {
            this.loadClients();
            if (result._generatedPassword) {
              alert('Mot de passe généré pour le client : ' + result._generatedPassword);
            }
          });
        }
      },
      () => {},
    );
  }

  openEditForm(client: AppUser): void {
    // On clone l'objet pour éviter la modification directe
    const modalRef = this.modalService.open(ClientsEditDialogComponent, { size: 'lg' });
    modalRef.componentInstance.client = { ...client };
    modalRef.result.then(
      (result: AppUser) => {
        if (result) {
          this.clientsService.update(result).subscribe(() => this.loadClients());
        }
      },
      () => {},
    );
  }

  deleteClient(client: AppUser): void {
    if (confirm(`Supprimer le client ${client.firstName} ${client.lastName} ?`)) {
      this.clientsService.delete(client.id).subscribe(() => this.loadClients());
    }
  }

  get filteredClients(): AppUser[] {
    // Avec la pagination côté serveur, on affiche directement les clients reçus
    return this.clients;
  }

  onPageChange(page: number): void {
    this.page = page;
    this.loadClients();
  }

  getRoleDisplayName(role?: string): string {
    switch (role) {
      case 'ADMIN':
        return 'Administrateur';
      case 'MANAGER':
        return 'Manager';
      case 'CLIENT':
        return 'Client';
      default:
        return 'Client';
    }
  }

  openRoleModal(client: AppUser): void {
    const modalRef = this.modalService.open(ClientsRoleDialogComponent, { size: 'md' });
    modalRef.componentInstance.client = { ...client };

    modalRef.componentInstance.roleChanged.subscribe((data: { client: AppUser; newRole: string }) => {
      this.updateUserRole(data.client, data.newRole);
    });
  }

  updateUserRole(client: AppUser, newRole: string): void {
    const updatedClient = { ...client, type: newRole };

    this.clientsService.updateRole(updatedClient).subscribe({
      next: () => {
        this.loadClients();
        // Optionnel : afficher un message de succès
        console.log(`Rôle de ${client.firstName} ${client.lastName} mis à jour vers ${newRole}`);
      },
      error: error => {
        console.error('Erreur lors de la mise à jour du rôle:', error);
        // Optionnel : afficher un message d'erreur
        alert('Erreur lors de la mise à jour du rôle');
      },
    });
  }
}
