import { Component } from '@angular/core';
import { RouterModule } from '@angular/router';
import { AccountService } from 'app/core/auth/account.service';
import { JsonPipe, CommonModule } from '@angular/common';

@Component({
  selector: 'app-sidebar',
  templateUrl: './sidebar.component.html',
  styleUrls: ['./sidebar.component.scss'],
  standalone: true,
  imports: [RouterModule, JsonPipe, CommonModule],
})
export class SidebarComponent {
  constructor(public accountService: AccountService) {}

  get isAdmin(): boolean {
    return this.accountService.hasAnyAuthority('ROLE_ADMIN');
  }
}
