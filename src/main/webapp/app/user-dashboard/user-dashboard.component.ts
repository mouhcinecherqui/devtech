import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';

@Component({
  selector: 'app-user-dashboard',
  templateUrl: './user-dashboard.component.html',
  styleUrls: ['./user-dashboard.component.scss'],
  standalone: true,
  imports: [CommonModule],
})
export class UserDashboardComponent {
  tickets = [
    { id: 18, title: 'Fix bix a problem', status: 'Open' },
    { id: 35, title: 'Treading in issues', status: 'In Progress' },
    { id: 28, title: 'Wrong threatening', status: 'Closed' },
    { id: 27, title: 'Framrk problem', status: 'Closed' },
    { id: 42, title: 'Fixcel reading noises', status: 'Closed' },
    { id: 43, title: 'Dangers in crise', status: 'Closed' },
  ];

  getStatusClass(status: string): string {
    const normalized = status.toLowerCase().replace(/\s+/g, '-');
    return normalized;
  }
}
