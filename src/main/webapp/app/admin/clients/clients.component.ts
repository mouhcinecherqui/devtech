import { Component, OnInit, inject } from '@angular/core';
import { ClientsService, AppUser } from './clients.service';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { ClientsAddDialogComponent } from './clients-add-dialog.component';
import { ClientsEditDialogComponent } from './clients-edit-dialog.component';

@Component({
  selector: 'app-admin-clients',
  templateUrl: './clients.component.html',
  styleUrls: ['./clients.component.scss'],
  standalone: true,
  imports: [CommonModule, FormsModule],
})
export class ClientsComponent implements OnInit {
  clients: AppUser[] = [];
  private readonly clientsService = inject(ClientsService);
  private readonly modalService = inject(NgbModal);

  ngOnInit(): void {
    this.loadClients();
  }

  loadClients(): void {
    this.clientsService.getAll().subscribe(clients => {
      this.clients = clients;
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
}
