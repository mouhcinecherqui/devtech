import { Injectable, inject } from '@angular/core';
import { Observable, BehaviorSubject, shareReplay } from 'rxjs';
import { AppParameter } from 'app/admin/parameters/parameters.model';
import { ParametersService } from 'app/admin/parameters/parameters.service';
import { map } from 'rxjs/operators';

@Injectable({ providedIn: 'root' })
export class AppParametersService {
  private readonly parametersService = inject(ParametersService);

  // Observables pour les différents types de paramètres
  private ticketStatuses$ = new BehaviorSubject<AppParameter[]>([]);
  private ticketTypes$ = new BehaviorSubject<AppParameter[]>([]);
  private ticketPriorities$ = new BehaviorSubject<AppParameter[]>([]);
  private otherParameters$ = new BehaviorSubject<AppParameter[]>([]);

  constructor() {
    this.initializeParameters();
  }

  private initializeParameters(): void {
    // Charger tous les paramètres au démarrage
    // Utiliser getPublicParameters() pour éviter les erreurs 403
    this.parametersService.getPublicParameters().subscribe({
      next: params => {
        this.ticketStatuses$.next(params.filter(p => p.type === 'ticket-status'));
        this.ticketTypes$.next(params.filter(p => p.type === 'ticket-type'));
        this.ticketPriorities$.next(params.filter(p => p.type === 'ticket-priority'));
        this.otherParameters$.next(params.filter(p => !['ticket-status', 'ticket-type', 'ticket-priority'].includes(p.type || '')));
      },
      error: error => {
        console.warn('Impossible de charger les paramètres publics:', error);
        // Utiliser des valeurs par défaut en cas d'erreur
        this.setDefaultParameters();
      },
    });
  }

  private setDefaultParameters(): void {
    // Valeurs par défaut en cas d'erreur de chargement
    this.ticketStatuses$.next([
      { id: 1, key: 'open', value: 'Ouvert', type: 'ticket-status', description: 'Ticket ouvert' },
      { id: 2, key: 'in-progress', value: 'En cours', type: 'ticket-status', description: 'Ticket en cours de traitement' },
      { id: 3, key: 'closed', value: 'Fermé', type: 'ticket-status', description: 'Ticket fermé' },
    ]);

    this.ticketTypes$.next([
      { id: 4, key: 'support', value: 'Support', type: 'ticket-type', description: 'Demande de support' },
      { id: 5, key: 'bug', value: 'Bug', type: 'ticket-type', description: 'Rapport de bug' },
      { id: 6, key: 'feature', value: 'Fonctionnalité', type: 'ticket-type', description: 'Demande de fonctionnalité' },
    ]);

    this.ticketPriorities$.next([
      { id: 7, key: 'low', value: 'Faible', type: 'ticket-priority', description: 'Priorité faible' },
      { id: 8, key: 'normal', value: 'Normale', type: 'ticket-priority', description: 'Priorité normale' },
      { id: 9, key: 'high', value: 'Élevée', type: 'ticket-priority', description: 'Priorité élevée' },
      { id: 10, key: 'urgent', value: 'Urgente', type: 'ticket-priority', description: 'Priorité urgente' },
    ]);
  }

  // Méthode pour récupérer tous les paramètres publics
  getPublicParameters(): Observable<AppParameter[]> {
    return this.parametersService.getPublicParameters();
  }

  // Méthodes pour récupérer les paramètres
  getTicketStatuses(): Observable<AppParameter[]> {
    return this.ticketStatuses$.asObservable().pipe(shareReplay(1));
  }

  getTicketTypes(): Observable<AppParameter[]> {
    return this.ticketTypes$.asObservable().pipe(shareReplay(1));
  }

  getTicketPriorities(): Observable<AppParameter[]> {
    return this.ticketPriorities$.asObservable().pipe(shareReplay(1));
  }

  getOtherParameters(): Observable<AppParameter[]> {
    return this.otherParameters$.asObservable().pipe(shareReplay(1));
  }

  // Méthodes pour obtenir des valeurs spécifiques
  getTicketStatusValue(key: string): Observable<string | undefined> {
    return this.getTicketStatuses().pipe(
      shareReplay(1),
      map(statuses => statuses.find(s => s.key === key)?.value),
    );
  }

  getTicketTypeValue(key: string): Observable<string | undefined> {
    return this.getTicketTypes().pipe(
      shareReplay(1),
      map(types => types.find(t => t.key === key)?.value),
    );
  }

  getTicketPriorityValue(key: string): Observable<string | undefined> {
    return this.getTicketPriorities().pipe(
      shareReplay(1),
      map(priorities => priorities.find(p => p.key === key)?.value),
    );
  }

  // Méthodes pour rafraîchir les paramètres
  refreshParameters(): void {
    this.parametersService.refreshCache();
    this.initializeParameters();
  }

  // Méthodes pour obtenir les valeurs actuelles (synchrones)
  getCurrentTicketStatuses(): AppParameter[] {
    return this.ticketStatuses$.value;
  }

  getCurrentTicketTypes(): AppParameter[] {
    return this.ticketTypes$.value;
  }

  getCurrentTicketPriorities(): AppParameter[] {
    return this.ticketPriorities$.value;
  }

  getCurrentOtherParameters(): AppParameter[] {
    return this.otherParameters$.value;
  }

  // Méthodes utilitaires
  getParameterByKey(key: string): Observable<AppParameter | undefined> {
    return this.getPublicParameters().pipe(map(params => params.find(p => p.key === key)));
  }

  getParameterValue(key: string): Observable<string | undefined> {
    return this.getPublicParameters().pipe(map(params => params.find(p => p.key === key)?.value));
  }

  // Méthodes pour les valeurs par défaut
  getDefaultTicketStatus(): Observable<string> {
    return this.getTicketStatusValue('default').pipe(map(value => value || 'En cours'));
  }

  getDefaultTicketType(): Observable<string> {
    return this.getTicketTypeValue('default').pipe(map(value => value || 'Support'));
  }

  getDefaultTicketPriority(): Observable<string> {
    return this.getTicketPriorityValue('default').pipe(map(value => value || 'Normal'));
  }
}
