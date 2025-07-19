import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

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
  private readonly resourceUrl = '/api/app-users';

  getAll(): Observable<AppUser[]> {
    return this.http.get<AppUser[]>(this.resourceUrl);
  }

  create(client: Omit<AppUser, 'id'>): Observable<AppUser> {
    return this.http.post<AppUser>(this.resourceUrl, client);
  }

  update(client: AppUser): Observable<AppUser> {
    return this.http.put<AppUser>(`${this.resourceUrl}/${client.id}`, client);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.resourceUrl}/${id}`);
  }

  login(email: string, password: string): Observable<AppUser> {
    return this.http.post<AppUser>(`${this.resourceUrl}/login`, { email, password });
  }
}
