import { Routes } from '@angular/router';
/* jhipster-needle-add-admin-module-import - JHipster will add admin modules imports here */
import { UserRouteAccessService } from 'app/core/auth/user-route-access.service';
import { TicketsComponent } from './tickets/tickets.component';
import { PaiementsComponent } from './paiements/paiements.component';
import { ClientsComponent } from './clients/clients.component';
import userManagementRoute from './user-management/user-management.route';

const routes: Routes = [
  {
    path: 'users',
    loadChildren: () => import('./user-management/user-management.route'),
    title: 'userManagement.home.title',
  },
  {
    path: 'docs',
    loadComponent: () => import('./docs/docs.component'),
    title: 'global.menu.admin.apidocs',
  },
  {
    path: 'configuration',
    loadComponent: () => import('./configuration/configuration.component'),
    title: 'configuration.title',
  },
  {
    path: 'health',
    loadComponent: () => import('./health/health.component'),
    title: 'health.title',
  },
  {
    path: 'logs',
    loadComponent: () => import('./logs/logs.component'),
    title: 'logs.title',
  },
  {
    path: 'metrics',
    loadComponent: () => import('./metrics/metrics.component'),
    title: 'metrics.title',
  },
  {
    path: 'tickets',
    component: TicketsComponent,
    canActivate: [UserRouteAccessService],
    data: { authorities: ['ROLE_ADMIN'], title: 'Tickets' },
  },
  {
    path: 'paiements',
    component: PaiementsComponent,
    canActivate: [UserRouteAccessService],
    data: { authorities: ['ROLE_ADMIN'], title: 'Paiements' },
  },
  {
    path: 'clients',
    component: ClientsComponent,
    canActivate: [UserRouteAccessService],
    data: { authorities: ['ROLE_ADMIN'], title: 'Clients' },
  },
  {
    path: 'parameters',
    loadComponent: () => import('./parameters/parameters.component').then(m => m.ParametersComponent),
    canActivate: [UserRouteAccessService],
    data: { authorities: ['ROLE_ADMIN'], title: 'Paramètres' },
  },
  /* jhipster-needle-add-admin-route - JHipster will add admin routes here */
];

export default routes;
