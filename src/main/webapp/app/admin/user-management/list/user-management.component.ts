import { Component, OnInit, inject, signal, ChangeDetectorRef } from '@angular/core';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { HttpHeaders, HttpResponse } from '@angular/common/http';
import { combineLatest } from 'rxjs';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';

import SharedModule from 'app/shared/shared.module';
import { SortByDirective, SortDirective, SortService, SortState, sortStateSignal } from 'app/shared/sort';
import { ITEMS_PER_PAGE } from 'app/config/pagination.constants';
import { SORT } from 'app/config/navigation.constants';
import { ItemCountComponent } from 'app/shared/pagination';
import { AccountService } from 'app/core/auth/account.service';
import { UserManagementService } from '../service/user-management.service';
import { User } from '../user-management.model';
import UserManagementDeleteDialogComponent from '../delete/user-management-delete-dialog.component';
import { UserManagementRoleDialogComponent } from '../role-dialog/user-management-role-dialog.component';
import { AutoRefreshService } from '../../../core/services/auto-refresh.service';
import { RefreshButtonComponent } from '../../../shared/components/refresh-button/refresh-button.component';
import { AlertService } from '../../../core/util/alert.service';

@Component({
  selector: 'jhi-user-mgmt',
  templateUrl: './user-management.component.html',
  styleUrls: ['./user-management.component.scss'],
  imports: [
    RouterModule,
    SharedModule,
    SortDirective,
    SortByDirective,
    ItemCountComponent,
    UserManagementRoleDialogComponent,
    RefreshButtonComponent,
  ],
})
export default class UserManagementComponent implements OnInit {
  currentAccount = inject(AccountService).trackCurrentAccount();
  users = signal<User[] | null>(null);
  isLoading = signal(false);
  totalItems = signal(0);
  itemsPerPage = ITEMS_PER_PAGE;
  page!: number;
  sortState = sortStateSignal({});

  private readonly userService = inject(UserManagementService);
  private readonly activatedRoute = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly sortService = inject(SortService);
  private readonly modalService = inject(NgbModal);
  private readonly cdr = inject(ChangeDetectorRef);
  private readonly autoRefreshService = inject(AutoRefreshService);
  private readonly alertService = inject(AlertService);

  ngOnInit(): void {
    this.handleNavigation();

    // Configurer l'actualisation automatique toutes les 30 secondes
    this.autoRefreshService.setRefreshInterval(30000);
    this.autoRefreshService.refreshTrigger$.subscribe(() => {
      this.handleNavigation();
    });
  }

  setActive(user: User, isActivated: boolean): void {
    this.userService.update({ ...user, activated: isActivated }).subscribe(() => this.loadAll());
  }

  trackIdentity(item: User): number {
    return item.id!;
  }

  deleteUser(user: User): void {
    const modalRef = this.modalService.open(UserManagementDeleteDialogComponent, { size: 'lg', backdrop: 'static' });
    modalRef.componentInstance.user = user;
    // unsubscribe not needed because closed completes on modal close
    modalRef.closed.subscribe(reason => {
      if (reason === 'deleted') {
        this.loadAll();
      }
    });
  }

  loadAll(): void {
    this.isLoading.set(true);
    this.userService
      .query({
        page: this.page - 1,
        size: this.itemsPerPage,
        sort: this.sortService.buildSortParam(this.sortState(), 'id'),
      })
      .subscribe({
        next: (res: HttpResponse<User[]>) => {
          this.isLoading.set(false);
          this.onSuccess(res.body, res.headers);
          this.cdr.detectChanges();
        },
        error: () => {
          this.isLoading.set(false);
          this.cdr.detectChanges();
        },
      });
  }

  transition(sortState?: SortState): void {
    this.router.navigate(['./'], {
      relativeTo: this.activatedRoute.parent,
      queryParams: {
        page: this.page,
        sort: this.sortService.buildSortParam(sortState ?? this.sortState()),
      },
    });
  }

  private handleNavigation(): void {
    combineLatest([this.activatedRoute.data, this.activatedRoute.queryParamMap]).subscribe(([data, params]) => {
      const page = params.get('page');
      this.page = +(page ?? 1);
      this.sortState.set(this.sortService.parseSortParam(params.get(SORT) ?? data.defaultSort));
      this.loadAll();
    });
  }

  private onSuccess(users: User[] | null, headers: HttpHeaders): void {
    this.totalItems.set(Number(headers.get('X-Total-Count')));
    this.users.set(users);
  }

  openRoleModal(user: User): void {
    // Récupérer les autorités disponibles
    this.userService.authorities().subscribe(authorities => {
      const modalRef = this.modalService.open(UserManagementRoleDialogComponent, { size: 'lg' });
      modalRef.componentInstance.user = user;
      modalRef.componentInstance.availableAuthorities = authorities;

      modalRef.componentInstance.rolesChanged.subscribe((data: { user: User; newRoles: string[] }) => {
        this.updateUserRoles(data.user, data.newRoles);
      });
    });
  }

  updateUserRoles(user: User, newRoles: string[]): void {
    if (!user.login) {
      console.error('Login utilisateur manquant');
      return;
    }

    this.userService.updateRoles(user.login, newRoles).subscribe({
      next: () => {
        this.loadAll();
        console.log(`Rôles de ${user.login} mis à jour vers: ${newRoles.join(', ')}`);
      },
      error: error => {
        console.error('Erreur lors de la mise à jour des rôles:', error);
        this.alertService.addAlert({
          type: 'danger',
          message: 'Erreur lors de la mise à jour des rôles',
          timeout: 5000,
        });
      },
    });
  }
}
