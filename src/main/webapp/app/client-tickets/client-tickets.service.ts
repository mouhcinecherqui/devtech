import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface ClientTicket {
  id?: number;
  title: string;
  description: string;
  status?: string;
  createdDate?: string;
}

@Injectable({ providedIn: 'root' })
export class ClientTicketsService {
  private readonly http = inject(HttpClient);
  private readonly resourceUrl = '/api/tickets';

  getMyTickets(): Observable<ClientTicket[]> {
    return this.http.get<ClientTicket[]>(this.resourceUrl);
  }

  createTicket(ticket: Omit<ClientTicket, 'id' | 'status' | 'createdDate'>): Observable<ClientTicket> {
    return this.http.post<ClientTicket>(this.resourceUrl, ticket);
  }
}
