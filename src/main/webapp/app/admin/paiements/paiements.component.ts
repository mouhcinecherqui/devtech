import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';

interface Paiement {
  id: number;
  user: string;
  amount: number;
  date: string;
  status: string;
}

@Component({
  selector: 'app-admin-paiements',
  templateUrl: './paiements.component.html',
  styleUrls: ['./paiements.component.scss'],
  imports: [CommonModule],
})
export class PaiementsComponent {
  paiements: Paiement[] = [
    { id: 1, user: 'Alice', amount: 100, date: '2024-06-01', status: 'Completed' },
    { id: 2, user: 'Bob', amount: 50, date: '2024-06-02', status: 'Pending' },
    { id: 3, user: 'Charlie', amount: 75, date: '2024-06-03', status: 'Failed' },
  ];
}
