import { Component, OnInit, Input, Output, EventEmitter, inject } from '@angular/core';
import { PaymentMethodService } from '../payment-methods/payment-method.service';
import { HttpClient } from '@angular/common/http';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { environment } from '../../environments/environment';

interface PaymentInfo {
  paymentRequired: boolean;
  amount: number;
  currency: string;
}

interface TicketPaymentStatus {
  paymentRequired: boolean;
  paymentAmount: number;
  paymentCurrency: string;
  paymentStatus: string;
  paymentType: string;
}

@Component({
  selector: 'jhi-ticket-payment',
  templateUrl: './ticket-payment.component.html',
  styleUrls: ['./ticket-payment.component.scss'],
  standalone: true,
  imports: [CommonModule, FormsModule],
})
export class TicketPaymentComponent implements OnInit {
  @Input() ticketId?: number;
  @Input() ticketType?: string;
  @Output() paymentCompleted = new EventEmitter<boolean>();

  paymentInfo?: PaymentInfo;
  paymentStatus?: TicketPaymentStatus;
  isLoading = false;
  errorMessage = '';
  showPaymentForm = false;

  private paymentMethodService = inject(PaymentMethodService);

  constructor(private http: HttpClient) {}

  ngOnInit(): void {
    if (this.ticketType) {
      this.checkPaymentRequired();
    }
    if (this.ticketId) {
      this.getPaymentStatus();
    }
  }

  /**
   * Vérifie si un paiement est requis pour ce type de ticket
   */
  checkPaymentRequired(): void {
    if (!this.ticketType) return;

    this.isLoading = true;
    this.http.get<PaymentInfo>(`/api/ticket-payments/check/${this.ticketType}`).subscribe({
      next: info => {
        this.paymentInfo = info;
        this.isLoading = false;
      },
      error: error => {
        console.error('Erreur lors de la vérification du paiement:', error);
        this.errorMessage = 'Erreur lors de la vérification du paiement';
        this.isLoading = false;
      },
    });
  }

  /**
   * Obtient le statut de paiement d'un ticket
   */
  getPaymentStatus(): void {
    if (!this.ticketId) return;

    this.isLoading = true;
    this.http.get<TicketPaymentStatus>(`/api/ticket-payments/status/${this.ticketId}`).subscribe({
      next: status => {
        this.paymentStatus = status;
        this.isLoading = false;
      },
      error: error => {
        console.error("Erreur lors de l'obtention du statut de paiement:", error);
        this.errorMessage = "Erreur lors de l'obtention du statut de paiement";
        this.isLoading = false;
      },
    });
  }

  /**
   * Configure un ticket pour nécessiter un paiement
   */
  configurePayment(paymentType: string): void {
    if (!this.ticketId) return;

    this.isLoading = true;
    this.http
      .post(`/api/ticket-payments/configure/${this.ticketId}`, null, {
        params: { paymentType },
      })
      .subscribe({
        next: () => {
          this.getPaymentStatus();
          this.isLoading = false;
        },
        error: error => {
          console.error('Erreur lors de la configuration du paiement:', error);
          this.errorMessage = 'Erreur lors de la configuration du paiement';
          this.isLoading = false;
        },
      });
  }

  /**
   * Affiche le formulaire de paiement
   */
  showPayment(): void {
    this.showPaymentForm = true;
  }

  /**
   * Masque le formulaire de paiement
   */
  hidePayment(): void {
    this.showPaymentForm = false;
  }

  /**
   * Obtient le texte du statut de paiement
   */
  getPaymentStatusText(status: string | undefined): string {
    if (!status) return 'Inconnu';

    switch (status) {
      case 'PENDING':
        return 'En attente de paiement';
      case 'COMPLETED':
        return 'Paiement complété';
      case 'FAILED':
        return 'Paiement échoué';
      default:
        return status;
    }
  }

  /**
   * Obtient la classe CSS pour le statut de paiement
   */
  getPaymentStatusClass(status: string | undefined): string {
    if (!status) return 'status-unknown';

    switch (status) {
      case 'PENDING':
        return 'status-pending';
      case 'COMPLETED':
        return 'status-completed';
      case 'FAILED':
        return 'status-failed';
      default:
        return 'status-unknown';
    }
  }
}
