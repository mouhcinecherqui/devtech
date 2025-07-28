import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { PaiementsService } from './paiements.service';
import { FilterPaiementsPipe } from './filterPaiements.pipe';
import SharedModule from '../../shared/shared.module';
import { IPaiement } from './paiement.model';

@Component({
  selector: 'app-admin-paiements',
  templateUrl: './paiements.component.html',
  styleUrls: ['./paiements.component.scss'],
  imports: [CommonModule, FormsModule, FilterPaiementsPipe, SharedModule],
})
export class PaiementsComponent implements OnInit {
  paiements: IPaiement[] = [];
  search = '';
  isLoading = false;

  constructor(private paiementsService: PaiementsService) {}

  ngOnInit(): void {
    this.loadPaiements();
  }

  loadPaiements(): void {
    this.isLoading = true;
    this.paiementsService.query().subscribe({
      next: response => {
        this.paiements = response.body || [];
        this.isLoading = false;
      },
      error: error => {
        console.error('Erreur lors du chargement des paiements:', error);
        this.isLoading = false;
      },
    });
  }

  get paiementsCompletes(): number {
    return this.paiements?.filter(p => p.status === 'COMPLETED' || p.status === 'Complété').length ?? 0;
  }

  get paiementsEnAttente(): number {
    return this.paiements?.filter(p => p.status === 'PENDING' || p.status === 'En attente').length ?? 0;
  }

  get paiementsEchoues(): number {
    return this.paiements?.filter(p => p.status === 'FAILED' || p.status === 'Échoué').length ?? 0;
  }

  get totalPaiements(): number {
    return this.paiements?.length ?? 0;
  }

  ajouterPaiement(): void {
    // Ouvre un dialog ou navigue vers une page d'ajout
    alert('Fonctionnalité à implémenter');
  }

  exporterFacturePDF(paiement: IPaiement): void {
    if (paiement.id) {
      this.paiementsService.downloadFacture(paiement.id).subscribe({
        next: (blob: Blob) => {
          const url = window.URL.createObjectURL(blob);
          const a = document.createElement('a');
          a.href = url;
          a.download = `facture-paiement-${paiement.id}.pdf`;
          a.click();
          window.URL.revokeObjectURL(url);
        },
        error: error => {
          console.error('Erreur lors du téléchargement de la facture:', error);
          alert('Erreur lors du téléchargement de la facture');
        },
      });
    }
  }

  actualiserListe(): void {
    this.loadPaiements();
  }

  getStatusClass(status: string | null | undefined): string {
    if (!status) return 'status-unknown';

    switch (status.toUpperCase()) {
      case 'COMPLETED':
      case 'COMPLÉTÉ':
        return 'status-success';
      case 'PENDING':
      case 'EN ATTENTE':
        return 'status-warning';
      case 'FAILED':
      case 'ÉCHOUÉ':
        return 'status-danger';
      default:
        return 'status-unknown';
    }
  }

  getStatusText(status: string | null | undefined): string {
    if (!status) return 'Inconnu';

    switch (status.toUpperCase()) {
      case 'COMPLETED':
      case 'COMPLÉTÉ':
        return 'Complété';
      case 'PENDING':
      case 'EN ATTENTE':
        return 'En attente';
      case 'FAILED':
      case 'ÉCHOUÉ':
        return 'Échoué';
      default:
        return status;
    }
  }
}
