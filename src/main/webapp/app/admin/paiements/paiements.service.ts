import { Injectable } from '@angular/core';
import { HttpClient, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';

import { ApplicationConfigService } from 'app/core/config/application-config.service';
import { createRequestOption } from 'app/core/request/request-util';
import { IPaiement, NewPaiement } from '../paiements/paiement.model';

export type PartialUpdatePaiement = Partial<IPaiement> & Pick<IPaiement, 'id'>;

export type EntityResponseType = HttpResponse<IPaiement>;
export type EntityArrayResponseType = HttpResponse<IPaiement[]>;

@Injectable({ providedIn: 'root' })
export class PaiementsService {
  protected resourceUrl = this.applicationConfigService.getEndpointFor('api/paiements');

  constructor(
    protected http: HttpClient,
    protected applicationConfigService: ApplicationConfigService,
  ) {}

  create(paiement: NewPaiement): Observable<EntityResponseType> {
    return this.http.post<IPaiement>(this.resourceUrl, paiement, { observe: 'response' });
  }

  update(paiement: IPaiement): Observable<EntityResponseType> {
    return this.http.put<IPaiement>(`${this.resourceUrl}/${this.getPaiementIdentifier(paiement)}`, paiement, {
      observe: 'response',
    });
  }

  partialUpdate(paiement: PartialUpdatePaiement): Observable<EntityResponseType> {
    return this.http.patch<IPaiement>(`${this.resourceUrl}/${this.getPaiementIdentifier(paiement)}`, paiement, {
      observe: 'response',
    });
  }

  find(id: number): Observable<EntityResponseType> {
    return this.http.get<IPaiement>(`${this.resourceUrl}/${id}`, { observe: 'response' });
  }

  query(req?: any): Observable<EntityArrayResponseType> {
    const options = createRequestOption(req);
    return this.http.get<IPaiement[]>(this.resourceUrl, { params: options, observe: 'response' });
  }

  delete(id: number): Observable<HttpResponse<{}>> {
    return this.http.delete(`${this.resourceUrl}/${id}`, { observe: 'response' });
  }

  getPaiementIdentifier(paiement: Pick<IPaiement, 'id'>): number {
    return paiement.id;
  }

  comparePaiement(o1: Pick<IPaiement, 'id'> | null, o2: Pick<IPaiement, 'id'> | null): boolean {
    return o1 && o2 ? this.getPaiementIdentifier(o1) === this.getPaiementIdentifier(o2) : o1 === o2;
  }

  // Méthodes CMI
  createPaymentRequest(paiement: any): Observable<any> {
    return this.http.post(`${this.resourceUrl}/create-payment`, paiement);
  }

  checkPaymentStatus(orderId: string): Observable<IPaiement> {
    return this.http.get<IPaiement>(`${this.resourceUrl}/status/check/${orderId}`);
  }

  getPaiementsByUser(user: string): Observable<IPaiement[]> {
    return this.http.get<IPaiement[]>(`${this.resourceUrl}/user/${user}`);
  }

  getPaiementsByStatus(status: string): Observable<IPaiement[]> {
    return this.http.get<IPaiement[]>(`${this.resourceUrl}/status/${status}`);
  }

  getRecentPaiements(): Observable<IPaiement[]> {
    return this.http.get<IPaiement[]>(`${this.resourceUrl}/recent`);
  }

  getPendingPaiements(): Observable<IPaiement[]> {
    return this.http.get<IPaiement[]>(`${this.resourceUrl}/pending`);
  }

  getTodayPaiements(): Observable<IPaiement[]> {
    return this.http.get<IPaiement[]>(`${this.resourceUrl}/today`);
  }

  getPaymentStats(): Observable<any> {
    return this.http.get<any>(`${this.resourceUrl}/stats`);
  }

  getPaiementsByDateRange(startDate: string, endDate: string): Observable<IPaiement[]> {
    return this.http.get<IPaiement[]>(`${this.resourceUrl}/date-range`, {
      params: { startDate, endDate },
    });
  }

  downloadFacture(id: number): Observable<Blob> {
    return this.http.get(`${this.resourceUrl}/${id}/facture`, {
      responseType: 'blob',
    });
  }
}
