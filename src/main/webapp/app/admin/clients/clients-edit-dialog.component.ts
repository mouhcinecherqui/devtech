import { Component, Input } from '@angular/core';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { AppUser } from './clients.service';

@Component({
  selector: 'jhi-clients-edit-dialog',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './clients-edit-dialog.component.html',
  styleUrls: ['./clients-edit-dialog.component.scss'],
})
export class ClientsEditDialogComponent {
  @Input() client!: AppUser;
  constructor(public activeModal: NgbActiveModal) {}

  submit(): void {
    if (!this.client.firstName || !this.client.lastName || !this.client.email || !this.client.phone || !this.client.password) return;
    this.activeModal.close(this.client);
  }
}
