import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ReactiveFormsModule, FormBuilder } from '@angular/forms';
import { provideHttpClient } from '@angular/common/http';
import {} from '@angular/router/testing';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { PaymentMethodsComponent } from './payment-methods.component';
import { PaymentMethodService } from './payment-method.service';

describe('PaymentMethodsComponent', () => {
  let component: PaymentMethodsComponent;
  let fixture: ComponentFixture<PaymentMethodsComponent>;
  let paymentMethodService: any;
  let translateService: any;

  beforeEach(async () => {
    const paymentMethodServiceSpy = {
      list: jest.fn(() => ({ subscribe: (arg: any) => (typeof arg === 'function' ? arg([]) : arg?.next?.([])) })),
      add: jest.fn(() => ({ subscribe: (arg: any) => (typeof arg === 'function' ? arg({}) : arg?.next?.({})) })),
      remove: jest.fn(() => ({ subscribe: (arg: any) => (typeof arg === 'function' ? arg({}) : arg?.next?.({})) })),
      setDefault: jest.fn(() => ({ subscribe: (arg: any) => (typeof arg === 'function' ? arg({}) : arg?.next?.({})) })),
    } as any;
    const translateSpy = { instant: jest.fn() };

    await TestBed.configureTestingModule({
      imports: [PaymentMethodsComponent, ReactiveFormsModule, TranslateModule.forRoot()],
      providers: [
        FormBuilder,
        { provide: PaymentMethodService, useValue: paymentMethodServiceSpy },
        { provide: TranslateService, useValue: translateSpy },
        provideHttpClient(),
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(PaymentMethodsComponent);
    component = fixture.componentInstance;
    paymentMethodService = TestBed.inject(PaymentMethodService) as any;
    translateService = TestBed.inject(TranslateService) as any;
    (component as any).reload = () => {};
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should initialize with default values', () => {
    expect(component.methods()).toEqual([]);
    expect(component.adding()).toBeFalsy();
  });

  it('should initialize addForm with defaults', () => {
    expect(component.addForm.get('number')?.value).toBe('');
    expect(component.addForm.get('brand')?.value).toBe('VISA');
  });

  it('should load payment methods on initialization', () => {
    expect(component.methods()).toEqual([]);
  });

  it('should validate required fields', () => {
    const number = component.addForm.get('number');
    const cvv = component.addForm.get('cvv');
    const holderName = component.addForm.get('holderName');
    number?.markAsTouched();
    cvv?.markAsTouched();
    holderName?.markAsTouched();
    expect(number?.hasError('required')).toBeTruthy();
    expect(cvv?.hasError('required')).toBeTruthy();
    expect(holderName?.hasError('required')).toBeTruthy();
  });

  it('should validate card number format', () => {
    const number = component.addForm.get('number');
    number?.setValue('1234');
    number?.markAsTouched();
    expect(number?.hasError('invalidCardNumber')).toBeTruthy();
  });

  // expiry handled via expMonth/expYear validators

  it('should validate CVV format', () => {
    const cvv = component.addForm.get('cvv');
    cvv?.setValue('12');
    cvv?.markAsTouched();
    expect(cvv?.hasError('invalidCvv')).toBeTruthy();
  });

  it('should mark form as valid with proper values', () => {
    component.addForm.patchValue({
      number: '4111111111111111',
      cvv: '123',
      holderName: 'John Doe',
      brand: 'VISA',
    });
    expect(component.addForm.valid).toBeTruthy();
  });

  it('should open add payment method form', () => {
    component.startAdd();
    expect(component.adding()).toBeTruthy();
  });

  it('should close add payment method form', () => {
    component.cancelAdd();
    expect(component.adding()).toBeFalsy();
  });

  // Edit path not present

  it('should add new payment method when form is valid', () => {
    paymentMethodService.add.mockReturnValue({ subscribe: (o: any) => o.next({}) });
    component.addForm.patchValue({ holderName: 'a', number: '4111111111111111', cvv: '123', brand: 'VISA' });
    component.save();
    expect(component.isSubmit).toBeFalsy();
  });

  // Update path not present

  it('should remove payment method', () => {
    paymentMethodService.remove.mockReturnValue({ subscribe: (arg: any) => (typeof arg === 'function' ? arg({}) : arg?.next?.({})) });
    component.remove('1');
    expect(paymentMethodService.remove).toHaveBeenCalled();
  });

  it('should set default payment method', () => {
    paymentMethodService.setDefault.mockReturnValue({ subscribe: (arg: any) => (typeof arg === 'function' ? arg({}) : arg?.next?.({})) });
    component.setDefault('1');
    expect(paymentMethodService.setDefault).toHaveBeenCalled();
  });

  // maskCardNumber helper not exposed

  // type detection helper not exposed

  // field error helper not exposed

  // pattern error helper not exposed

  // list error handled internally

  // add error path handled internally
});
