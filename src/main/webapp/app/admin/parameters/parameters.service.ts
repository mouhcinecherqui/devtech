import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, BehaviorSubject, shareReplay, tap, map } from 'rxjs';
import { AppParameter } from './parameters.model';

@Injectable({ providedIn: 'root' })
export class ParametersService {
  private readonly http = inject(HttpClient);
  private readonly resourceUrl = '/api/admin/app-parameters';

  // Cache pour les paramètres
  private parametersCache$ = new BehaviorSubject<AppParameter[]>([]);
  private lastFetch = 0;
  private readonly CACHE_DURATION = 5 * 60 * 1000; // 5 minutes

  getAll(): Observable<AppParameter[]> {
    const now = Date.now();

    // Retourner le cache si il est encore valide
    if (this.lastFetch > 0 && now - this.lastFetch < this.CACHE_DURATION) {
      return this.parametersCache$.asObservable();
    }

    // Récupérer depuis le serveur et mettre en cache
    return this.http.get<AppParameter[]>(this.resourceUrl).pipe(
      tap(params => {
        this.parametersCache$.next(params);
        this.lastFetch = now;
      }),
      shareReplay(1),
    );
  }

  // Méthode publique pour les clients (sans authentification admin)
  getPublicParameters(): Observable<AppParameter[]> {
    const now = Date.now();

    // Retourner le cache si il est encore valide
    if (this.lastFetch > 0 && now - this.lastFetch < this.CACHE_DURATION) {
      return this.parametersCache$.asObservable();
    }

    // Récupérer les paramètres publics depuis le serveur
    const publicUrl = '/api/public/parameters';
    return this.http.get<AppParameter[]>(publicUrl).pipe(
      tap(params => {
        this.parametersCache$.next(params);
        this.lastFetch = now;
      }),
      shareReplay(1),
    );
  }

  get(id: number): Observable<AppParameter> {
    return this.http.get<AppParameter>(`${this.resourceUrl}/${id}`);
  }

  create(param: AppParameter): Observable<AppParameter> {
    return this.http.post<AppParameter>(this.resourceUrl, param).pipe(
      tap(newParam => {
        // Mettre à jour le cache
        const currentParams = this.parametersCache$.value;
        this.parametersCache$.next([...currentParams, newParam]);
        this.lastFetch = Date.now();
      }),
    );
  }

  update(param: AppParameter): Observable<AppParameter> {
    return this.http.put<AppParameter>(`${this.resourceUrl}/${param.id}`, param).pipe(
      tap(updatedParam => {
        // Mettre à jour le cache
        const currentParams = this.parametersCache$.value;
        const updatedParams = currentParams.map(p => (p.id === updatedParam.id ? updatedParam : p));
        this.parametersCache$.next(updatedParams);
        this.lastFetch = Date.now();
      }),
    );
  }

  delete(id: number): Observable<unknown> {
    return this.http.delete<unknown>(`${this.resourceUrl}/${id}`).pipe(
      tap(() => {
        // Mettre à jour le cache
        const currentParams = this.parametersCache$.value;
        const filteredParams = currentParams.filter(p => p.id !== id);
        this.parametersCache$.next(filteredParams);
        this.lastFetch = Date.now();
      }),
    );
  }

  // Méthodes utilitaires pour récupérer des paramètres spécifiques
  getByType(type: string): Observable<AppParameter[]> {
    return this.getPublicParameters().pipe(map(params => params.filter(p => p.type === type)));
  }

  getByKey(key: string): Observable<AppParameter | undefined> {
    return this.getPublicParameters().pipe(map(params => params.find(p => p.key === key)));
  }

  // Méthodes pour les paramètres spécifiques
  getTicketStatuses(): Observable<AppParameter[]> {
    return this.getByType('ticket-status');
  }

  getTicketTypes(): Observable<AppParameter[]> {
    return this.getByType('ticket-type');
  }

  getTicketPriorities(): Observable<AppParameter[]> {
    return this.getByType('ticket-priority');
  }

  // Forcer le rafraîchissement du cache
  refreshCache(): void {
    this.lastFetch = 0;
  }

  // Vider le cache
  clearCache(): void {
    this.parametersCache$.next([]);
    this.lastFetch = 0;
  }

  // Obtenir la valeur actuelle du cache
  getCachedParameters(): AppParameter[] {
    return this.parametersCache$.value;
  }
}
