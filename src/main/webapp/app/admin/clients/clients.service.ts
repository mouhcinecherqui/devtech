import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';
import { createRequestOption } from '../../core/request/request-util';
import { ApplicationConfigService } from '../../core/config/application-config.service';
import { Pagination } from '../../core/request/request.model';

export interface AppUser {
  id: number;
  firstName: string;
  lastName: string;
  email: string;
  phone: string;
  password: string;
  type?: string;
  createdDate?: string;
}

@Injectable({ providedIn: 'root' })
export class ClientsService {
  private readonly http = inject(HttpClient);
  private readonly applicationConfigService = inject(ApplicationConfigService);
  private readonly resourceUrl = this.applicationConfigService.getEndpointFor('api/app-users');

  getAll(req?: Pagination): Observable<HttpResponse<AppUser[]>> {
    const options = createRequestOption(req);
    return this.http.get<AppUser[]>(this.resourceUrl, { params: options, observe: 'response' });
  }

  create(client: Omit<AppUser, 'id'>): Observable<AppUser> {
    return this.http.post<AppUser>(this.resourceUrl, client);
  }

  update(client: AppUser): Observable<AppUser> {
    return this.http.put<AppUser>(`${this.resourceUrl}/${client.id}`, client);
  }

  delete(id: number): Observable<unknown> {
    return this.http.delete<unknown>(`${this.resourceUrl}/${id}`);
  }

  login(email: string, password: string): Observable<AppUser> {
    return this.http.post<AppUser>(`${this.resourceUrl}/login`, { email, password });
  }

  updateRole(client: AppUser): Observable<AppUser> {
    return this.http.put<AppUser>(`${this.resourceUrl}/${client.id}/role`, { type: client.type });
  }
}
