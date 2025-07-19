import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class PaiementsService {
  constructor(private http: HttpClient) {}

  getPaiements(): Observable<any[]> {
    return this.http.get<any[]>('/api/paiements');
  }

  exportFacturePDF(paiementId: number): Observable<Blob> {
    return this.http.get(`/api/paiements/${paiementId}/facture`, { responseType: 'blob' });
  }
}
