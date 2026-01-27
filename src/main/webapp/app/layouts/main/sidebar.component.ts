import { Component, inject, OnInit, OnDestroy } from '@angular/core';
import { Router, RouterModule } from '@angular/router';
import { AccountService } from 'app/core/auth/account.service';
import { JsonPipe, CommonModule } from '@angular/common';
import SharedModule from '../../shared/shared.module';
import { SidebarService } from './sidebar.service';
import { LoginService } from 'app/login/login.service';
import { Subject, takeUntil } from 'rxjs';

@Component({
  selector: 'jhi-sidebar',
  templateUrl: './sidebar.component.html',
  styleUrls: ['./sidebar.component.scss'],
  standalone: true,
  imports: [RouterModule, JsonPipe, CommonModule, SharedModule],
})
export class SidebarComponent implements OnInit, OnDestroy {
  constructor(public accountService: AccountService) {}
  readonly sidebarService = inject(SidebarService);
  private readonly loginService = inject(LoginService);
  private readonly router = inject(Router);
  private readonly destroy$ = new Subject<void>();

  sidebarOpen = false;

  ngOnInit(): void {
    this.sidebarService.sidebarOpen$.pipe(takeUntil(this.destroy$)).subscribe(open => {
      this.sidebarOpen = open;
    });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  closeSidebarOnMobile(): void {
    // Fermer la sidebar sur mobile quand on clique sur un lien
    if (window.innerWidth <= 768) {
      this.sidebarService.closeSidebar();
    }
  }

  logout(): void {
    this.closeSidebarOnMobile();
    this.loginService.logout();
    this.router.navigate(['']);
  }

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
