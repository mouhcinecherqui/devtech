import { Component, OnInit, inject } from '@angular/core';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { NgbAlert } from '@ng-bootstrap/ng-bootstrap';

import SharedModule from 'app/shared/shared.module';
import { PaiementsService } from 'app/admin/paiements/paiements.service';
import { AccountService } from 'app/core/auth/account.service';
import { TranslateService } from '@ngx-translate/core';

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
  private readonly paypalLinkBase = 'https://www.paypal.com/paypalme/devtechly/';
  currentAccount = inject(AccountService).trackCurrentAccount();

  private formBuilder = inject(FormBuilder);
  private paiementsService = inject(PaiementsService);
  private router = inject(Router);
  private translateService = inject(TranslateService);

  ngOnInit(): void {
    this.initForm();
  }

  private initForm(): void {
    this.paymentForm = this.formBuilder.group({
      amount: ['', [Validators.required, Validators.min(1), Validators.max(100000)]],
      currency: ['MAD', Validators.required],
      description: ['', [Validators.required, Validators.maxLength(500)]],
      date: [new Date().toISOString().split('T')[0], Validators.required],
      paymentMethod: ['bank_transfer', Validators.required],
    });
  }

  onPaymentMethodChange(method: 'bank_transfer' | 'paypal'): void {
    this.paymentForm.patchValue({ paymentMethod: method });
  }

  get selectedPaymentMethod(): string {
    return this.paymentForm?.get('paymentMethod')?.value;
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

      if (paymentData.paymentMethod === 'paypal') {
        this.handlePaypalPayment(paymentData);
      } else {
        this.handleBankTransferPayment(paymentData);
      }
    }
  }

  onCancel(): void {
    this.router.navigate(['/client-dashboard']);
  }

  private handleBankTransferPayment(paymentData: any): void {
    this.paiementsService.createPaymentRequest(paymentData).subscribe({
      next: () => {
        this.isLoading = false;
        this.successMessage = this.translateService.instant('clientPayment.messages.createSuccessBankTransfer');
        this.paymentForm.reset();
        this.initForm();
      },
      error: error => this.handlePaymentError(error),
    });
  }

  private handlePaypalPayment(paymentData: any): void {
    this.paiementsService.createPaymentRequest(paymentData).subscribe({
      next: () => {
        this.isLoading = false;
        this.successMessage = this.translateService.instant('clientPayment.messages.createSuccessPaypal');
        this.openPaypalWindow(paymentData.amount);
        this.paymentForm.reset();
        this.initForm();
        this.paymentForm.patchValue({ paymentMethod: 'paypal' });
      },
      error: error => this.handlePaymentError(error),
    });
  }

  private openPaypalWindow(amount: number): void {
    const numericAmount = Number(amount) || 0;
    const paypalUrl = `${this.paypalLinkBase}${numericAmount}`;
    window.open(paypalUrl, '_blank');
  }

  private handlePaymentError(error: any): void {
    this.isLoading = false;
    const translatedError = this.translateService.instant('clientPayment.messages.createError', {
      error: error.message || this.translateService.instant('clientPayment.messages.unknownError'),
    });
    this.errorMessage = translatedError;
  }

  getErrorMessage(fieldName: string): string {
    const field = this.paymentForm.get(fieldName);
    if (field?.errors) {
      if (field.errors['required']) {
        return this.translateService.instant('clientPayment.validation.required');
      }
      if (field.errors['min']) {
        return this.translateService.instant('clientPayment.validation.amountMin', {
          min: field.errors['min'].min,
        });
      }
      if (field.errors['max']) {
        return this.translateService.instant('clientPayment.validation.amountMax', {
          max: field.errors['max'].max,
        });
      }
      if (field.errors['maxlength']) {
        return this.translateService.instant('clientPayment.validation.descriptionMax', {
          max: field.errors['maxlength'].requiredLength,
        });
      }
    }
    return '';
  }
}
