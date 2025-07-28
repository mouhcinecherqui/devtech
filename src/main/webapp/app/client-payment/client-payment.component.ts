import { Component, OnInit, inject } from '@angular/core';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { NgbAlert } from '@ng-bootstrap/ng-bootstrap';

import SharedModule from 'app/shared/shared.module';
import { PaiementsService } from 'app/admin/paiements/paiements.service';
import { AccountService } from 'app/core/auth/account.service';

@Component({
  selector: 'app-client-payment',
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

  constructor(
    private formBuilder: FormBuilder,
    private paiementsService: PaiementsService,
    private router: Router,
  ) {}

  ngOnInit(): void {
    this.initForm();
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
          this.errorMessage = 'Erreur lors de la création de la demande de paiement: ' + error.message;
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
}
