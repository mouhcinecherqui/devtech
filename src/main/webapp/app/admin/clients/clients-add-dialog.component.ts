import { Component, Input } from '@angular/core';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { AppUser } from './clients.service';

@Component({
  selector: 'jhi-clients-add-dialog',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './clients-add-dialog.component.html',
  styleUrls: ['./clients-add-dialog.component.scss'],
})
export class ClientsAddDialogComponent {
  @Input() client: Partial<AppUser> = { firstName: '', lastName: '', email: '', phone: '' };
  constructor(public activeModal: NgbActiveModal) {}

  private generatePassword(length = 10): string {
    const chars = 'abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789!@#$%^&*';
    let pwd = '';
    for (let i = 0; i < length; i++) {
      pwd += chars.charAt(Math.floor(Math.random() * chars.length));
    }
    return pwd;
  }

  submit(): void {
    if (!this.client.firstName || !this.client.lastName || !this.client.email || !this.client.phone) return;
    const generatedPassword = this.generatePassword();
    this.activeModal.close({ ...this.client, password: generatedPassword, type: 'client', _generatedPassword: generatedPassword });
  }
}
