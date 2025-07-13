import { Component } from '@angular/core';

interface Ticket {
  id: number;
  title: string;
  status: string;
  created: string;
}

@Component({
  selector: 'app-admin-tickets',
  templateUrl: './tickets.component.html',
  styleUrls: ['./tickets.component.scss'],
})
export class TicketsComponent {
  tickets: Ticket[] = [
    { id: 1, title: 'Login issue', status: 'Open', created: '2024-06-01' },
    { id: 2, title: 'Payment failed', status: 'Closed', created: '2024-06-02' },
    { id: 3, title: 'Feature request', status: 'Pending', created: '2024-06-03' },
  ];
}
