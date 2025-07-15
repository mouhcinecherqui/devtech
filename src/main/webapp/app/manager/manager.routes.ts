import { Routes } from '@angular/router';
import { UserRouteAccessService } from 'app/core/auth/user-route-access.service';
import { TicketsComponent } from '../admin/tickets/tickets.component';
import { ClientsComponent } from '../admin/clients/clients.component';
import { ParametersComponent } from '../admin/parameters/parameters.component';

const routes: Routes = [
  {
    path: 'user-management',
    loadChildren: () => import('../admin/user-management/user-management.route'),
    title: 'userManagement.home.title',
  },
  {
    path: 'docs',
    loadComponent: () => import('../admin/docs/docs.component'),
    title: 'global.menu.admin.apidocs',
  },
  {
    path: 'configuration',
    loadComponent: () => import('../admin/configuration/configuration.component'),
    title: 'configuration.title',
  },
  {
    path: 'health',
    loadComponent: () => import('../admin/health/health.component'),
    title: 'health.title',
  },
  {
    path: 'logs',
    loadComponent: () => import('../admin/logs/logs.component'),
    title: 'logs.title',
  },
  {
    path: 'metrics',
    loadComponent: () => import('../admin/metrics/metrics.component'),
    title: 'metrics.title',
  },
  {
    path: 'tickets',
    component: TicketsComponent,
    canActivate: [UserRouteAccessService],
    data: { authorities: ['ROLE_MANAGER'], title: 'Tickets' },
  },
  {
    path: 'clients',
    component: ClientsComponent,
    canActivate: [UserRouteAccessService],
    data: { authorities: ['ROLE_MANAGER'], title: 'Clients' },
  },
  {
    path: 'parameters',
    component: ParametersComponent,
    canActivate: [UserRouteAccessService],
    data: { authorities: ['ROLE_MANAGER'], title: 'Paramètres' },
  },
];

export default routes;
