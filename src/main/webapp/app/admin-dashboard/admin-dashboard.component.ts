import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { SidebarComponent } from '../layouts/main/sidebar.component';

@Component({
  selector: 'app-admin-dashboard',
  templateUrl: './admin-dashboard.component.html',
  styleUrls: ['./admin-dashboard.component.scss'],
  standalone: true,
  imports: [CommonModule, SidebarComponent],
})
export class AdminDashboardComponent {
  stats = {
    tickets: 10,
    open: 3,
    inProgress: 5,
    closed: 2,
  };
  tickets = [
    { id: 41, title: 'Fix bix a problem', author: 'user', created: 'Apr 21, 2022, at 1:55 AM' },
    { id: 28, title: 'Threaten it issues', author: 'admin', created: 'Apr 21, 2022, at 1:34 AM' },
    { id: 34, title: 'Wrong threatening', author: 'Jordon', created: 'Apr 21, 2022, at 1:34 AM' },
    { id: 45, title: 'Fixcel reading noise-admin', author: 'admin', created: 'Apr 21, 2022, at 1:34 AM' },
  ];
}
