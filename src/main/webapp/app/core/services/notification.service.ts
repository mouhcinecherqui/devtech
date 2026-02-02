import { Injectable, inject, signal, computed } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable, timer, Subscription, interval, of } from 'rxjs';
import { takeUntil, switchMap, tap, catchError, map } from 'rxjs';
import { AccountService } from '../auth/account.service';

export interface Notification {
  id: number;
  title: string;
  message: string;
  type: 'info' | 'success' | 'warning' | 'error';
  /** Type brut du backend (TICKET_CREATED, MESSAGE_RECEIVED, etc.) pour icônes */
  rawType?: string;
  timestamp: string;
  read: boolean;
  userId?: number;
  ticketId?: number;
  paymentId?: number;
  actionUrl?: string;
  createdDate?: string;
}

export interface NotificationStats {
  total: number;
  unread: number;
  byType: {
    info: number;
    success: number;
    warning: number;
    error: number;
  };
}

@Injectable({
  providedIn: 'root',
})
export class NotificationService {
  private readonly http = inject(HttpClient);
  private readonly accountService = inject(AccountService);

  // Signals pour l'état des notifications
  private readonly notifications = signal<Notification[]>([]);
  private readonly isLoading = signal(false);
  private readonly lastFetch = signal<Date | null>(null);

  // Observable pour le rafraîchissement automatique
  private refreshInterval$ = new BehaviorSubject<number>(5000); // 5 secondes par défaut
  private isEnabled$ = new BehaviorSubject<boolean>(true);
  private destroy$ = new BehaviorSubject<boolean>(false);

  // Computed signals
  public readonly notifications$ = computed(() => this.notifications());
  public readonly isLoading$ = computed(() => this.isLoading());
  public readonly unreadCount$ = computed(() => this.notifications().filter(n => !n.read).length);
  public readonly notificationStats$ = computed(() => {
    const notifs = this.notifications();
    return {
      total: notifs.length,
      unread: notifs.filter(n => !n.read).length,
      byType: {
        info: notifs.filter(n => n.type === 'info').length,
        success: notifs.filter(n => n.type === 'success').length,
        warning: notifs.filter(n => n.type === 'warning').length,
        error: notifs.filter(n => n.type === 'error').length,
      },
    };
  });

  constructor() {
    console.log('🔔 Démarrage du système de notifications global...');
    this.startAutoRefresh();
    // Charger les vraies notifications depuis l'API
    this.fetchNotifications().subscribe();
  }

  /**
   * Démarrer l'actualisation automatique des notifications
   */
  private startAutoRefresh(): void {
    this.isEnabled$
      .pipe(
        switchMap(enabled =>
          enabled ? interval(this.refreshInterval$.value).pipe(takeUntil(this.destroy$)) : timer(0).pipe(takeUntil(this.destroy$)),
        ),
      )
      .subscribe(() => {
        this.fetchNotifications();
      });
  }

  /**
   * Récupérer les notifications depuis l'API
   */
  fetchNotifications(): Observable<Notification[]> {
    this.isLoading.set(true);
    console.log('🔔 Début du chargement des notifications...');

    return this.http.get<Notification[]>('/api/notifications').pipe(
      tap(notifications => {
        console.log('📡 Réponse API reçue:', notifications);
        console.log('📡 Type de la réponse:', typeof notifications);
        console.log('📡 Nombre de notifications:', notifications?.length || 0);

        // S'assurer que nous avons un tableau
        const notificationsArray = Array.isArray(notifications) ? notifications : [];

        // Convertir les notifications du backend vers le format frontend
        const convertedNotifications = notificationsArray.map(notif => ({
          id: notif.id,
          title: notif.title || 'Notification',
          message: notif.message || 'Message par défaut',
          type: this.mapNotificationType(notif.type),
          rawType: notif.type,
          timestamp: notif.timestamp || notif.createdDate || new Date().toISOString(),
          read: notif.read || false,
          userId: notif.userId,
          ticketId: notif.ticketId,
          paymentId: notif.paymentId,
          actionUrl: notif.actionUrl,
        }));

        console.log('🔄 Notifications converties:', convertedNotifications);

        this.notifications.set(convertedNotifications);
        this.lastFetch.set(new Date());
        this.isLoading.set(false);
        console.log('✅ Notifications mises à jour avec succès:', convertedNotifications.length);
      }),
      catchError(error => {
        console.error('❌ Erreur lors du chargement des notifications:', error);
        console.error("❌ Détails de l'erreur:", {
          status: error.status,
          statusText: error.statusText,
          message: error.message,
          url: error.url,
        });
        this.isLoading.set(false);

        // En cas d'erreur 401 (non authentifié), ne pas afficher d'erreur
        if (error.status === 401) {
          console.log('🔒 Utilisateur non authentifié, notifications ignorées');
          return of([]);
        }

        // Pour les autres erreurs, afficher un message d'erreur mais continuer
        console.warn('⚠️ Erreur réseau ou serveur, utilisation de données en cache');
        return of([]);
      }),
    );
  }

  /**
   * Marquer une notification comme lue
   */
  markAsRead(notificationId: number): Observable<void> {
    console.log('📖 Marquage de la notification comme lue:', notificationId);

    return this.http.put<void>(`/api/notifications/${notificationId}/read`, {}).pipe(
      tap(() => {
        console.log('✅ Notification marquée comme lue avec succès:', notificationId);
        const notifs = this.notifications();
        const updatedNotifs = notifs.map(n => (n.id === notificationId ? { ...n, read: true } : n));
        this.notifications.set(updatedNotifs);
      }),
      catchError(error => {
        console.error('❌ Erreur lors du marquage de la notification:', error);
        console.error('❌ Détails:', {
          status: error.status,
          statusText: error.statusText,
          message: error.message,
        });
        throw error;
      }),
    );
  }

  /**
   * Marquer toutes les notifications comme lues
   */
  markAllAsRead(): Observable<void> {
    console.log('📖 Marquage de toutes les notifications comme lues');

    return this.http.put<void>('/api/notifications/read-all', {}).pipe(
      tap(() => {
        console.log('✅ Toutes les notifications marquées comme lues avec succès');
        const notifs = this.notifications();
        const updatedNotifs = notifs.map(n => ({ ...n, read: true }));
        this.notifications.set(updatedNotifs);
      }),
      catchError(error => {
        console.error('❌ Erreur lors du marquage de toutes les notifications:', error);
        console.error('❌ Détails:', {
          status: error.status,
          statusText: error.statusText,
          message: error.message,
        });
        throw error;
      }),
    );
  }

  /**
   * Supprimer une notification
   */
  deleteNotification(notificationId: number): Observable<void> {
    console.log('🗑️ Suppression de la notification:', notificationId);

    return this.http.delete<void>(`/api/notifications/${notificationId}`).pipe(
      tap(() => {
        console.log('✅ Notification supprimée avec succès:', notificationId);
        const notifs = this.notifications();
        const updatedNotifs = notifs.filter(n => n.id !== notificationId);
        this.notifications.set(updatedNotifs);
      }),
      catchError(error => {
        console.error('❌ Erreur lors de la suppression de la notification:', error);
        console.error('❌ Détails:', {
          status: error.status,
          statusText: error.statusText,
          message: error.message,
        });
        throw error;
      }),
    );
  }

  /**
   * Mapper les types de notifications du backend vers le frontend
   */
  private mapNotificationType(backendType: string): 'info' | 'success' | 'warning' | 'error' {
    switch (backendType) {
      case 'TICKET_CREATED':
      case 'PAYMENT_RECEIVED':
      case 'PAYMENT_CONFIRMED':
        return 'success';
      case 'TICKET_UPDATED':
      case 'MESSAGE_RECEIVED':
        return 'info';
      case 'PAYMENT_PENDING':
        return 'warning';
      case 'TICKET_ERROR':
      case 'PAYMENT_FAILED':
        return 'error';
      default:
        return 'info';
    }
  }

  /**
   * Configurer l'intervalle de rafraîchissement
   */
  setRefreshInterval(intervalMs: number): void {
    this.refreshInterval$.next(intervalMs);
  }

  /**
   * Activer/désactiver l'actualisation automatique
   */
  setEnabled(enabled: boolean): void {
    this.isEnabled$.next(enabled);
  }

  /**
   * Forcer un rafraîchissement immédiat
   */
  forceRefresh(): void {
    this.fetchNotifications().subscribe();
  }

  /**
   * Détruire le service
   */
  destroy(): void {
    this.destroy$.next(true);
  }
}
