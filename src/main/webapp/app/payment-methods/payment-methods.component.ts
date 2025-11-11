import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import SharedModule from 'app/shared/shared.module';
import { PaymentMethodService, PaymentMethod } from './payment-method.service';

@Component({
  selector: 'jhi-payment-methods',
  standalone: true,
  templateUrl: './payment-methods.component.html',
  styleUrls: ['./payment-methods.component.scss'],
  imports: [CommonModule, SharedModule, FormsModule, ReactiveFormsModule],
})
export class PaymentMethodsComponent implements OnInit {
  private paymentService = inject(PaymentMethodService);
  methods = signal<PaymentMethod[]>([]);
  adding = signal(false);
  isSubmit = false;

  // Custom validators
  cardNumberValidator = (control: any): any => {
    if (!control.value) return null;

    const value = control.value.replace(/\s/g, '');
    const cardNumberPattern = /^[0-9]{13,19}$/;

    if (!cardNumberPattern.test(value)) {
      return { invalidCardNumber: true };
    }

    // Luhn algorithm validation
    if (!this.luhnCheck(value)) {
      return { invalidCardNumber: true };
    }

    return null;
  };

  cvvValidator = (control: any): any => {
    if (!control.value) return null;

    const cvvPattern = /^\d{3,4}$/;
    if (!cvvPattern.test(control.value)) {
      return { invalidCvv: true };
    }

    return null;
  };

  addForm = inject(FormBuilder).group({
    holderName: ['', [Validators.required, Validators.maxLength(80)]],
    brand: ['VISA', Validators.required],
    number: ['', [Validators.required, this.cardNumberValidator]],
    expMonth: [1, [Validators.required, Validators.min(1), Validators.max(12)]],
    expYear: [new Date().getFullYear(), [Validators.required, Validators.min(new Date().getFullYear())]],
    cvv: ['', [Validators.required, this.cvvValidator]],
    billingAddress: [''],
    billingCity: [''],
    billingCountry: [''],
    billingZip: [''],
    makeDefault: [true],
  });

  ngOnInit(): void {
    this.reload();
    this.setupCardNumberFormatting();
  }

  private setupCardNumberFormatting(): void {
    const numberControl = this.addForm.get('number');
    if (numberControl) {
      numberControl.valueChanges.subscribe(value => {
        if (value) {
          const formatted = this.formatCardNumber(value);
          if (formatted !== value) {
            numberControl.setValue(formatted, { emitEvent: false });
          }
        }
      });
    }
  }

  private formatCardNumber(value: string): string {
    const cleaned = value.replace(/\s/g, '');
    const groups = cleaned.match(/.{1,4}/g);
    return groups ? groups.join(' ') : cleaned;
  }

  reload(): void {
    this.paymentService.list().subscribe(methods => this.methods.set(methods));
  }

  startAdd(): void {
    this.adding.set(true);
  }

  cancelAdd(): void {
    this.adding.set(false);
    this.addForm.reset({ brand: 'VISA', expMonth: 1, expYear: new Date().getFullYear(), makeDefault: true });
  }

  save(): void {
    if (this.addForm.invalid) return;

    this.isSubmit = true;
    const { holderName, brand, number, expMonth, expYear, makeDefault } = this.addForm.getRawValue();
    const last4 = (number as string).slice(-4);

    this.paymentService
      .add(
        {
          holderName: holderName!,
          brand: brand!,
          last4,
          expMonth: Number(expMonth),
          expYear: Number(expYear),
          type: 'CARD',
        },
        !!makeDefault,
      )
      .subscribe({
        next: () => {
          this.cancelAdd();
          this.reload();
          this.isSubmit = false;
        },
        error: () => {
          this.isSubmit = false;
        },
      });
  }

  setDefault(id: string): void {
    this.paymentService.setDefault(id).subscribe(() => this.reload());
  }

  remove(id: string): void {
    this.paymentService.remove(id).subscribe(() => this.reload());
  }

  private luhnCheck(cardNumber: string): boolean {
    let sum = 0;
    let isEven = false;

    for (let i = cardNumber.length - 1; i >= 0; i--) {
      let digit = parseInt(cardNumber.charAt(i), 10);

      if (isEven) {
        digit *= 2;
        if (digit > 9) {
          digit -= 9;
        }
      }

      sum += digit;
      isEven = !isEven;
    }

    return sum % 10 === 0;
  }
}
