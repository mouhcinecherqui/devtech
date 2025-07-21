import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { PaiementsService } from './paiements.service';
import { FilterPaiementsPipe } from './filterPaiements.pipe';
import SharedModule from '../../shared/shared.module';

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
  imports: [CommonModule, FormsModule, FilterPaiementsPipe, SharedModule],
})
export class PaiementsComponent implements OnInit {
  paiements: Paiement[] = [];
  search = '';

  constructor(private paiementsService: PaiementsService) {}

  ngOnInit(): void {
    this.loadPaiements();
  }

  loadPaiements(): void {
    this.paiementsService.getPaiements().subscribe(data => {
      this.paiements = data;
    });
  }

  get paiementsCompletes(): number {
    return this.paiements?.filter(p => p.status === 'Completed' || p.status === 'Complété').length ?? 0;
  }
  get paiementsEnAttente(): number {
    return this.paiements?.filter(p => p.status === 'Pending' || p.status === 'En attente').length ?? 0;
  }
  get paiementsEchoues(): number {
    return this.paiements?.filter(p => p.status === 'Failed' || p.status === 'Échoué').length ?? 0;
  }

  ajouterPaiement(): void {
    // Ouvre un dialog ou navigue vers une page d'ajout
    alert('Fonctionnalité à implémenter');
  }

  exporterFacturePDF(paiement: Paiement): void {
    this.paiementsService.exportFacturePDF(paiement.id).subscribe(blob => {
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `facture-paiement-${paiement.id}.pdf`;
      a.click();
      window.URL.revokeObjectURL(url);
    });
  }
}
