import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';

import { ApplicationConfigService } from 'app/core/config/application-config.service';
import { createRequestOption } from 'app/core/request/request-util';
import { Pagination } from 'app/core/request/request.model';

export interface ClientReviewDTO {
  id?: number;
  clientName: string;
  rating: number;
  comment?: string;
  createdDate?: string;
  updatedDate?: string;
  isApproved?: boolean;
  ticketId?: number;
  ticketTitle?: string;
}

export interface ReviewStats {
  averageRating: number;
  totalReviews: number;
}

@Injectable({ providedIn: 'root' })
export class ClientReviewService {
  protected resourceUrl: string;

  private readonly http = inject(HttpClient);
  private readonly applicationConfigService = inject(ApplicationConfigService);

  constructor() {
    this.resourceUrl = this.applicationConfigService.getEndpointFor('api/client-reviews');
  }

  create(clientReview: ClientReviewDTO): Observable<ClientReviewDTO> {
    return this.http.post<ClientReviewDTO>(this.resourceUrl, clientReview);
  }

  update(clientReview: ClientReviewDTO): Observable<ClientReviewDTO> {
    return this.http.put<ClientReviewDTO>(`${this.resourceUrl}/${clientReview.id}`, clientReview);
  }

  partialUpdate(clientReview: ClientReviewDTO): Observable<ClientReviewDTO> {
    return this.http.patch<ClientReviewDTO>(`${this.resourceUrl}/${clientReview.id}`, clientReview);
  }

  find(id: number): Observable<ClientReviewDTO> {
    return this.http.get<ClientReviewDTO>(`${this.resourceUrl}/${id}`);
  }

  query(req?: Pagination): Observable<HttpResponse<ClientReviewDTO[]>> {
    const options = createRequestOption(req);
    return this.http.get<ClientReviewDTO[]>(this.resourceUrl, { params: options, observe: 'response' });
  }

  delete(id: number): Observable<{}> {
    return this.http.delete(`${this.resourceUrl}/${id}`);
  }

  // Méthodes spécifiques pour les avis clients

  findByTicketId(ticketId: number): Observable<ClientReviewDTO> {
    return this.http.get<ClientReviewDTO>(`${this.resourceUrl}/ticket/${ticketId}`);
  }

  getApprovedReviews(req?: Pagination): Observable<HttpResponse<ClientReviewDTO[]>> {
    const options = createRequestOption(req);
    return this.http.get<ClientReviewDTO[]>(`${this.resourceUrl}/approved`, { params: options, observe: 'response' });
  }

  getAllApprovedReviews(): Observable<ClientReviewDTO[]> {
    return this.http.get<ClientReviewDTO[]>(`${this.resourceUrl}/approved/all`);
  }

  getPublicApprovedReviews(): Observable<ClientReviewDTO[]> {
    return this.http.get<ClientReviewDTO[]>(`${this.resourceUrl}/public/approved`);
  }

  getReviewStats(): Observable<ReviewStats> {
    return this.http.get<ReviewStats>(`${this.resourceUrl}/stats`);
  }

  getPublicReviewStats(): Observable<ReviewStats> {
    return this.http.get<ReviewStats>(`${this.resourceUrl}/public/stats`);
  }

  getPendingReviews(): Observable<ClientReviewDTO[]> {
    return this.http.get<ClientReviewDTO[]>(`${this.resourceUrl}/pending`);
  }

  approveReview(id: number): Observable<ClientReviewDTO> {
    return this.http.post<ClientReviewDTO>(`${this.resourceUrl}/${id}/approve`, {});
  }

  rejectReview(id: number): Observable<ClientReviewDTO> {
    return this.http.post<ClientReviewDTO>(`${this.resourceUrl}/${id}/reject`, {});
  }

  // Méthodes utilitaires

  createReviewForTicket(ticketId: number, clientName: string, rating: number, comment?: string): Observable<ClientReviewDTO> {
    const review: ClientReviewDTO = {
      ticketId,
      clientName,
      rating,
      comment,
      isApproved: true,
    };
    return this.create(review);
  }

  getAverageRating(): Observable<number> {
    return new Observable(observer => {
      this.getReviewStats().subscribe({
        next: stats => observer.next(stats.averageRating),
        error: error => observer.error(error),
      });
    });
  }

  getTotalReviews(): Observable<number> {
    return new Observable(observer => {
      this.getReviewStats().subscribe({
        next: stats => observer.next(stats.totalReviews),
        error: error => observer.error(error),
      });
    });
  }
}
