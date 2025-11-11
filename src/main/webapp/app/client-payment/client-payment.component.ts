import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { NgbAlert } from '@ng-bootstrap/ng-bootstrap';

import SharedModule from 'app/shared/shared.module';
import { PaiementsService } from 'app/admin/paiements/paiements.service';
import { AccountService } from 'app/core/auth/account.service';
import { PaymentMethodService, PaymentMethod } from 'app/payment-methods/payment-method.service';

@Component({
  selector: 'jhi-client-payment',
  templateUrl: './client-payment.component.html',
  styleUrls: ['./client-payment.component.scss'],
  standalone: true,
  imports: [CommonModule, SharedModule, ReactiveFormsModule, NgbAlert],
})
export class ClientPaymentComponent implements OnInit {
  paymentForm!: FormGroup;
  isLoading = false;
  errorMessage = '';
  successMessage = '';
  currentAccount = inject(AccountService).trackCurrentAccount();

  // Payment methods
  private paymentMethodService = inject(PaymentMethodService);
  private formBuilder = inject(FormBuilder);
  private paiementsService = inject(PaiementsService);
  private router = inject(Router);

  savedPaymentMethods = signal<PaymentMethod[]>([]);
  selectedPaymentMethod: PaymentMethod | null = null;
  useSavedPayment = false;
  showNewPaymentForm = false;

  ngOnInit(): void {
    this.initForm();
    this.loadSavedPaymentMethods();
  }

  private initForm(): void {
    this.paymentForm = this.formBuilder.group({
      amount: ['', [Validators.required, Validators.min(1), Validators.max(100000)]],
      currency: ['MAD', Validators.required],
      description: ['', [Validators.required, Validators.maxLength(500)]],
      date: [new Date().toISOString().split('T')[0], Validators.required],
    });
  }

  onSubmit(): void {
    if (this.paymentForm.valid) {
      this.isLoading = true;
      this.errorMessage = '';
      this.successMessage = '';

      const paymentData = {
        ...this.paymentForm.value,
        user: this.currentAccount()?.login || 'anonymous',
      };

      this.paiementsService.createPaymentRequest(paymentData).subscribe({
        next: (response: any) => {
          this.isLoading = false;
          this.successMessage = 'Demande de paiement créée avec succès. Redirection vers la passerelle de paiement...';

          // Créer un formulaire temporaire pour rediriger vers CMI
          this.submitToCmi(response);
        },
        error: error => {
          this.isLoading = false;
          this.errorMessage = 'Erreur lors de la création de la demande de paiement: ' + (error.message || 'Erreur inconnue');
        },
      });
    }
  }

  private submitToCmi(paymentParams: any): void {
    // Créer un formulaire temporaire pour soumettre à CMI
    const form = document.createElement('form');
    form.method = 'POST';
    form.action = 'https://testpayment.cmi.co.ma/fim/est3Dgate'; // URL de test CMI

    // Ajouter tous les paramètres
    Object.keys(paymentParams).forEach(key => {
      const input = document.createElement('input');
      input.type = 'hidden';
      input.name = key;
      input.value = paymentParams[key];
      form.appendChild(input);
    });

    // Ajouter le formulaire au DOM et le soumettre
    document.body.appendChild(form);
    form.submit();
    document.body.removeChild(form);
  }

  onCancel(): void {
    this.router.navigate(['/client-dashboard']);
  }

  getErrorMessage(fieldName: string): string {
    const field = this.paymentForm.get(fieldName);
    if (field?.errors) {
      if (field.errors['required']) {
        return 'Ce champ est requis';
      }
      if (field.errors['min']) {
        return `Le montant minimum est ${field.errors['min'].min}`;
      }
      if (field.errors['max']) {
        return `Le montant maximum est ${field.errors['max'].max}`;
      }
      if (field.errors['maxlength']) {
        return `La description ne peut pas dépasser ${field.errors['maxlength'].requiredLength} caractères`;
      }
    }
    return '';
  }

  // Payment methods management
  loadSavedPaymentMethods(): void {
    this.paymentMethodService.list().subscribe({
      next: methods => {
        this.savedPaymentMethods.set(methods);
        // Auto-select default payment method if available
        const defaultMethod = methods.find(m => m.isDefault);
        if (defaultMethod) {
          this.selectSavedPaymentMethod(defaultMethod);
        }
      },
      error: (error: any) => {
        console.warn('Erreur lors du chargement des moyens de paiement:', error);
      },
    });
  }

  selectSavedPaymentMethod(method: PaymentMethod): void {
    this.selectedPaymentMethod = method;
    this.useSavedPayment = true;
    this.showNewPaymentForm = false;
  }

  useNewPaymentMethod(): void {
    this.useSavedPayment = false;
    this.selectedPaymentMethod = null;
    this.showNewPaymentForm = true;
  }

  getPaymentMethodDisplayName(method: PaymentMethod): string {
    return `${method.brand} •••• ${method.last4} (${method.expMonth}/${method.expYear})`;
  }

  getPaymentMethodIcon(brand: string): string {
    switch (brand.toLowerCase()) {
      case 'visa':
        return '💳';
      case 'mastercard':
        return '💳';
      case 'amex':
        return '💳';
      default:
        return '💳';
    }
  }
}
