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
import { AlertService } from '../../core/util/alert.service';
import { ConfirmDialogComponent } from '../../shared/components/confirm-dialog/confirm-dialog.component';

@Component({
  selector: 'jhi-admin-clients',
  templateUrl: './clients.component.html',
  styleUrls: ['./clients.component.scss'],
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    SharedModule,
    RefreshButtonComponent,
    NgbPaginationModule,
    ItemCountComponent,
    ConfirmDialogComponent,
  ],
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
  private readonly alertService = inject(AlertService);

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
              this.alertService.addAlert({
                type: 'info',
                message: 'Mot de passe généré pour le client : ' + result._generatedPassword,
                timeout: 10000,
              });
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
    const modalRef = this.modalService.open(ConfirmDialogComponent, { size: 'md' });
    modalRef.componentInstance.title = 'Confirmation de suppression';
    modalRef.componentInstance.message = `Supprimer le client ${client.firstName} ${client.lastName} ?`;
    modalRef.result.then(
      (confirmed: boolean) => {
        if (confirmed) {
          this.clientsService.delete(client.id).subscribe({
            next: () => {
              this.loadClients();
              this.alertService.addAlert({
                type: 'success',
                message: `Client ${client.firstName} ${client.lastName} supprimé avec succès`,
                timeout: 3000,
              });
            },
            error: () => {
              this.alertService.addAlert({
                type: 'danger',
                message: 'Erreur lors de la suppression du client',
                timeout: 5000,
              });
            },
          });
        }
      },
      () => {},
    );
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
        this.alertService.addAlert({
          type: 'danger',
          message: 'Erreur lors de la mise à jour du rôle',
          timeout: 5000,
        });
      },
    });
  }
}
