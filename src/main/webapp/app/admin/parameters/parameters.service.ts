import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { AppParameter } from './parameters.model';

@Injectable({ providedIn: 'root' })
export class ParametersService {
  private readonly http = inject(HttpClient);
  private readonly resourceUrl = '/api/admin/app-parameters';

  getAll(): Observable<AppParameter[]> {
    return this.http.get<AppParameter[]>(this.resourceUrl);
  }

  get(id: number): Observable<AppParameter> {
    return this.http.get<AppParameter>(`${this.resourceUrl}/${id}`);
  }

  create(param: AppParameter): Observable<AppParameter> {
    return this.http.post<AppParameter>(this.resourceUrl, param);
  }

  update(param: AppParameter): Observable<AppParameter> {
    return this.http.put<AppParameter>(`${this.resourceUrl}/${param.id}`, param);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.resourceUrl}/${id}`);
  }
}
