import { Routes } from '@angular/router';

import { Authority } from 'app/config/authority.constants';

import { UserRouteAccessService } from 'app/core/auth/user-route-access.service';
import { errorRoute } from './layouts/error/error.route';

const routes: Routes = [
  {
    path: '',
    loadComponent: () => import('./home/home.component'),
    title: 'home.title',
  },
  {
    path: '',
    loadComponent: () => import('./layouts/navbar/navbar.component'),
    outlet: 'navbar',
  },
  {
    path: 'admin',
    data: {
      authorities: [Authority.ADMIN],
    },
    canActivate: [UserRouteAccessService],
    loadChildren: () => import('./admin/admin.routes'),
  },
  {
    path: 'manager',
    data: {
      authorities: [Authority.MANAGER],
    },
    canActivate: [UserRouteAccessService],
    loadChildren: () => import('./manager/manager.routes'),
  },
  {
    path: 'account',
    loadChildren: () => import('./account/account.route'),
  },
  {
    path: 'login',
    loadComponent: () => import('./login/login.component'),
    title: 'login.title',
  },
  {
    path: '',
    loadChildren: () => import(`./entities/entity.routes`),
  },
  {
    path: 'bug-report',
    loadComponent: () => import('./bug-report/bug-report.component').then(m => m.BugReportComponent),
    title: 'Bug Report',
  },
  {
    path: 'user-dashboard',
    loadComponent: () => import('./user-dashboard/user-dashboard.component').then(m => m.UserDashboardComponent),
    canActivate: [UserRouteAccessService],
    data: { authorities: [Authority.USER] },
    title: 'User Dashboard',
  },
  {
    path: 'admin-dashboard',
    loadComponent: () => import('./admin-dashboard/admin-dashboard.component').then(m => m.AdminDashboardComponent),
    canActivate: [UserRouteAccessService],
    data: { authorities: [Authority.ADMIN] },
    title: 'Admin Dashboard',
  },
  {
    path: 'manager-dashboard',
    loadComponent: () => import('./manager-dashboard/manager-dashboard.component').then(m => m.ManagerDashboardComponent),
    canActivate: [UserRouteAccessService],
    data: { authorities: [Authority.MANAGER] },
    title: 'Manager Dashboard',
  },
  {
    path: 'app-user-register',
    loadComponent: () => import('./app-user-register/app-user-register.component').then(m => m.AppUserRegisterComponent),
    title: 'Register New User',
  },
  {
    path: 'user-tickets',
    loadComponent: () => import('./user-tickets/user-tickets.component').then(m => m.UserTicketsComponent),
    canActivate: [UserRouteAccessService],
    data: { authorities: [Authority.USER] },
    title: 'Mes tickets',
  },
  ...errorRoute,
];

export default routes;
