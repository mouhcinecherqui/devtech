import { Component } from '@angular/core';
import { RouterModule } from '@angular/router';
import { AccountService } from 'app/core/auth/account.service';
import { JsonPipe, CommonModule } from '@angular/common';
import SharedModule from '../../shared/shared.module';

@Component({
  selector: 'jhi-sidebar',
  templateUrl: './sidebar.component.html',
  styleUrls: ['./sidebar.component.scss'],
  standalone: true,
  imports: [RouterModule, JsonPipe, CommonModule, SharedModule],
})
export class SidebarComponent {
  constructor(public accountService: AccountService) {}

  get isAdmin(): boolean {
    return this.accountService.hasAnyAuthority('ROLE_ADMIN');
  }

  get isManager(): boolean {
    return this.accountService.hasAnyAuthority('ROLE_MANAGER');
  }

  get isAdminOrManager(): boolean {
    return this.isAdmin || this.isManager;
  }

  get isUser(): boolean {
    return this.accountService.hasAnyAuthority('ROLE_USER') && !this.isAdmin && !this.isManager;
  }

  get isClient(): boolean {
    return this.accountService.hasAnyAuthority('ROLE_CLIENT') && !this.isAdmin && !this.isManager;
  }
}
