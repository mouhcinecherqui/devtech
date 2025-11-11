import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ApplicationConfigService } from 'app/core/config/application-config.service';

export type PaymentMethodType = 'CARD';

export interface PaymentMethod {
  id: string;
  type: PaymentMethodType;
  brand: string;
  last4: string;
  expMonth: number;
  expYear: number;
  holderName: string;
  isDefault: boolean;
}

@Injectable({ providedIn: 'root' })
export class PaymentMethodService {
  private http = inject(HttpClient);
  private appConfig = inject(ApplicationConfigService);

  list(): Observable<PaymentMethod[]> {
    return this.http.get<PaymentMethod[]>(this.appConfig.getEndpointFor('api/payment-methods'));
  }

  add(method: Omit<PaymentMethod, 'id' | 'isDefault'>, makeDefault = true): Observable<PaymentMethod> {
    const body: Partial<PaymentMethod> = { ...method, isDefault: makeDefault } as any;
    return this.http.post<PaymentMethod>(this.appConfig.getEndpointFor('api/payment-methods'), body);
  }

  remove(id: string | number): Observable<unknown> {
    return this.http.delete<unknown>(this.appConfig.getEndpointFor(`api/payment-methods/${id}`));
  }

  setDefault(id: string | number): Observable<unknown> {
    return this.http.post<unknown>(this.appConfig.getEndpointFor(`api/payment-methods/set-default/${id}`), {});
  }

  getDefault(): Observable<PaymentMethod[]> {
    return this.list();
  }

  hasDefault(): boolean {
    // This method remains for compatibility in ticket-payment, but it requires async usage now.
    // Prefer calling list() and checking client-side.
    return false;
  }
}
