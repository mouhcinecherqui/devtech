import { Component, OnInit, inject } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { NgbAlert } from '@ng-bootstrap/ng-bootstrap';

import SharedModule from 'app/shared/shared.module';
import { PaiementsService } from 'app/admin/paiements/paiements.service';
import { AccountService } from 'app/core/auth/account.service';

@Component({
  selector: 'jhi-payment-result',
  templateUrl: './payment-result.component.html',
  styleUrls: ['./payment-result.component.scss'],
  standalone: true,
  imports: [CommonModule, SharedModule, NgbAlert],
})
export class PaymentResultComponent implements OnInit {
  paymentStatus: 'success' | 'failed' | 'pending' = 'pending';
  paymentDetails: any = null;
  isLoading = true;
  errorMessage = '';
  currentAccount = inject(AccountService).trackCurrentAccount();

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private paiementsService: PaiementsService,
  ) {}

  ngOnInit(): void {
    this.checkPaymentStatus();
  }

  private checkPaymentStatus(): void {
    // Récupérer les paramètres de l'URL
    this.route.queryParams.subscribe(params => {
      const orderId = params['oid'];
      const response = params['Response'];
      const errorMsg = params['ErrMsg'];

      if (orderId) {
        // Vérifier le statut du paiement
        this.paiementsService.checkPaymentStatus(orderId).subscribe({
          next: payment => {
            this.paymentDetails = payment;
            this.paymentStatus = this.determinePaymentStatus(payment, response, errorMsg);
            this.isLoading = false;
          },
          error: error => {
            this.errorMessage = 'Erreur lors de la vérification du statut de paiement: ' + (error.message || 'Erreur inconnue');
            this.isLoading = false;
            this.paymentStatus = 'failed';
          },
        });
      } else {
        // Pas d'ID de commande, considérer comme échec
        this.paymentStatus = 'failed';
        this.isLoading = false;
        this.errorMessage = "Aucun ID de commande trouvé dans l'URL";
      }
    });
  }

  private determinePaymentStatus(payment: any, response: string, errorMsg: string): 'success' | 'failed' | 'pending' {
    if (payment.status === 'COMPLETED') {
      return 'success';
    } else if (payment.status === 'FAILED' || response === 'Declined' || errorMsg) {
      return 'failed';
    } else {
      return 'pending';
    }
  }

  onDownloadInvoice(): void {
    if (this.paymentDetails?.id) {
      this.paiementsService.downloadFacture(this.paymentDetails.id).subscribe({
        next: blob => {
          const url = window.URL.createObjectURL(blob);
          const link = document.createElement('a');
          link.href = url;
          link.download = `facture-paiement-${this.paymentDetails.id}.pdf`;
          link.click();
          window.URL.revokeObjectURL(url);
        },
        error: error => {
          this.errorMessage = 'Erreur lors du téléchargement de la facture: ' + (error.message || 'Erreur inconnue');
        },
      });
    }
  }

  onGoToDashboard(): void {
    const account = this.currentAccount();
    if (account?.authorities?.includes('ROLE_ADMIN')) {
      this.router.navigate(['/admin-dashboard']);
    } else if (account?.authorities?.includes('ROLE_CLIENT')) {
      this.router.navigate(['/client-dashboard']);
    } else if (account?.authorities?.includes('ROLE_MANAGER')) {
      this.router.navigate(['/manager-dashboard']);
    } else if (account?.authorities?.includes('ROLE_USER')) {
      this.router.navigate(['/user-dashboard']);
    } else {
      this.router.navigate(['/home']);
    }
  }

  onTryAgain(): void {
    this.router.navigate(['/client-payment']);
  }

  onContactSupport(): void {
    // Rediriger vers la page de contact ou ouvrir un email
    window.open('mailto:contact.devtechly@gmail.com?subject=Problème de paiement', '_blank');
  }
}
