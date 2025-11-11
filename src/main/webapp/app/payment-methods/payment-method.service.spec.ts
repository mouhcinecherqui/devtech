import { TestBed } from '@angular/core/testing';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { PaymentMethodService } from './payment-method.service';

describe('PaymentMethodService', () => {
  let service: PaymentMethodService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [PaymentMethodService, provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(PaymentMethodService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should get payment methods', () => {
    const mockPaymentMethods = [
      { id: '1', type: 'CARD', brand: 'VISA', last4: '1234', expMonth: 12, expYear: 25, holderName: 'A', isDefault: true },
    ];

    let received: any;
    service.list().subscribe(m => (received = m));

    const req = httpMock.expectOne('api/payment-methods');
    expect(req.request.method).toBe('GET');
    req.flush(mockPaymentMethods);

    expect(received).toEqual(mockPaymentMethods);
  });

  it('should add a new payment method', () => {
    const newPaymentMethod: any = { holderName: 'John', brand: 'VISA', last4: '1111', expMonth: 12, expYear: 25, type: 'CARD' };
    const mockResponse = { id: '1', isDefault: true, ...newPaymentMethod };

    let received: any;
    service.add(newPaymentMethod, true).subscribe(m => (received = m));

    const req = httpMock.expectOne('api/payment-methods');
    expect(req.request.method).toBe('POST');
    req.flush(mockResponse);
    expect(received).toEqual(mockResponse);
  });

  // update API not present

  it('should delete a payment method', () => {
    service.remove('1').subscribe(() => expect(true).toBeTruthy());
    const req = httpMock.expectOne('api/payment-methods/1');
    expect(req.request.method).toBe('DELETE');
    req.flush({});
  });

  it('should set default payment method', () => {
    service.setDefault('1').subscribe(() => expect(true).toBeTruthy());
    const req = httpMock.expectOne('api/payment-methods/set-default/1');
    expect(req.request.method).toBe('POST');
    req.flush({});
  });

  // validate card endpoint not present

  it('should handle error when getting payment methods', () => {
    let errorCaught = false;
    service.list().subscribe({ error: () => (errorCaught = true) });
    const req = httpMock.expectOne('api/payment-methods');
    req.error(new ProgressEvent('Network error'));
    expect(errorCaught).toBeTruthy();
  });

  it('should handle error when adding payment method', () => {
    let errorCaught = false;
    service.add({} as any).subscribe({ error: () => (errorCaught = true) });
    const req = httpMock.expectOne('api/payment-methods');
    req.error(new ProgressEvent('Validation error'));
    expect(errorCaught).toBeTruthy();
  });

  // update error path not applicable

  it('should handle error when deleting payment method', () => {
    let errorCaught = false;
    service.remove('1').subscribe({ error: () => (errorCaught = true) });
    const req = httpMock.expectOne('api/payment-methods/1');
    req.error(new ProgressEvent('Delete error'));
    expect(errorCaught).toBeTruthy();
  });

  it('should handle error when setting default payment method', () => {
    let errorCaught = false;
    service.setDefault('1').subscribe({ error: () => (errorCaught = true) });
    const req = httpMock.expectOne('api/payment-methods/set-default/1');
    req.error(new ProgressEvent('Set default error'));
    expect(errorCaught).toBeTruthy();
  });

  // validate card endpoint not applicable
});
