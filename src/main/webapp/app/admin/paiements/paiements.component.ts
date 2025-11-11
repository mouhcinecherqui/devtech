import { Component, OnInit, inject, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { PaiementsService } from './paiements.service';
import { FilterPaiementsPipe } from './filterPaiements.pipe';
import { AutoRefreshService } from '../../core/services/auto-refresh.service';
import { RefreshButtonComponent } from '../../shared/components/refresh-button/refresh-button.component';
import SharedModule from '../../shared/shared.module';
import { IPaiement } from './paiement.model';
import { AddPaiementModalComponent } from './add-paiement-modal.component';

@Component({
  selector: 'jhi-admin-paiements',
  templateUrl: './paiements.component.html',
  styleUrls: ['./paiements.component.scss'],
  imports: [CommonModule, FormsModule, FilterPaiementsPipe, SharedModule, RefreshButtonComponent],
})
export class PaiementsComponent implements OnInit {
  paiements: IPaiement[] = [];
  search = '';
  isLoading = false;

  private readonly paiementsService = inject(PaiementsService);
  private readonly modalService = inject(NgbModal);
  private readonly cdr = inject(ChangeDetectorRef);
  private readonly autoRefreshService = inject(AutoRefreshService);

  ngOnInit(): void {
    this.loadPaiements();

    // Configurer l'actualisation automatique toutes les 30 secondes
    this.autoRefreshService.setRefreshInterval(30000);
    this.autoRefreshService.refreshTrigger$.subscribe(() => {
      this.loadPaiements();
    });
  }

  loadPaiements(): void {
    this.isLoading = true;
    this.paiementsService.query().subscribe({
      next: response => {
        this.paiements = response.body || [];
        this.isLoading = false;
        this.cdr.detectChanges();
      },
      error: error => {
        console.error('Erreur lors du chargement des paiements:', error);
        this.isLoading = false;
        this.cdr.detectChanges();
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
    const modalRef = this.modalService.open(AddPaiementModalComponent, {
      size: 'lg',
      backdrop: 'static',
      keyboard: false,
    });

    modalRef.result.then(
      result => {
        if (result) {
          // Ajouter le nouveau paiement à la liste
          this.paiements.unshift(result);
          console.warn('Paiement ajouté avec succès:', result);
        }
      },
      (reason: unknown) => {
        console.warn('Modal fermé:', reason);
      },
    );
  }

  exporterFacturePDF(paiement: IPaiement): void {
    if (paiement.id) {
      this.paiementsService.downloadFacture(paiement.id).subscribe({
        next(blob: Blob) {
          const url = window.URL.createObjectURL(blob);
          const a = document.createElement('a');
          a.href = url;
          a.download = `facture-paiement-${paiement.id}.pdf`;
          a.click();
          window.URL.revokeObjectURL(url);
        },
        error(error) {
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
