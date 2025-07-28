import { Component, OnInit, Input, Output, EventEmitter } from '@angular/core';
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
   * Crée une demande de paiement pour un ticket
   */
  createPaymentRequest(): void {
    if (!this.ticketId) return;

    this.isLoading = true;
    this.http.post<{ [key: string]: string }>(`/api/ticket-payments/create/${this.ticketId}`, {}).subscribe({
      next: paymentRequest => {
        // Rediriger vers la passerelle CMI
        this.redirectToCmiGateway(paymentRequest);
        this.isLoading = false;
      },
      error: error => {
        console.error('Erreur lors de la création de la demande de paiement:', error);
        this.errorMessage = 'Erreur lors de la création de la demande de paiement';
        this.isLoading = false;
      },
    });
  }

  /**
   * Redirige vers la passerelle CMI
   */
  private redirectToCmiGateway(paymentRequest: { [key: string]: string }): void {
    // Créer un formulaire temporaire pour soumettre les données CMI
    const form = document.createElement('form');
    form.method = 'POST';
    form.action = paymentRequest.gatewayUrl || 'https://testpayment.cmi.co.ma/fim/est3Dgate';
    form.target = '_blank';

    // Ajouter tous les paramètres CMI
    Object.keys(paymentRequest).forEach(key => {
      if (key !== 'gatewayUrl' && paymentRequest[key]) {
        const input = document.createElement('input');
        input.type = 'hidden';
        input.name = key;
        input.value = paymentRequest[key];
        form.appendChild(input);
      }
    });

    // Soumettre le formulaire
    document.body.appendChild(form);
    form.submit();
    document.body.removeChild(form);
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
