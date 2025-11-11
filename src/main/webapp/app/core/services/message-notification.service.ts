import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable, interval, timer } from 'rxjs';
import { takeUntil, switchMap, tap, catchError, map, filter } from 'rxjs';
import { NotificationService } from './notification.service';
import { AccountService } from '../auth/account.service';

export interface MessageNotification {
  id: number;
  ticketId: number;
  message: string;
  sender: string;
  timestamp: string;
  isFromClient: boolean;
  clientName?: string;
  ticketTitle?: string;
}

@Injectable({
  providedIn: 'root',
})
export class MessageNotificationService {
  private readonly http = inject(HttpClient);
  private readonly notificationService = inject(NotificationService);
  private readonly accountService = inject(AccountService);

  // Observable pour les nouveaux messages
  private newMessages$ = new BehaviorSubject<MessageNotification[]>([]);
  private lastMessageCheck = new Date();
  private isListening = false;

  // Configuration
  private readonly CHECK_INTERVAL = 5000; // Vérifier toutes les 5 secondes
  private readonly MAX_RETRIES = 3;

  constructor() {
    // Démarrer automatiquement l'écoute des notifications
    console.log('🚀 Démarrage du système de notifications de messages...');
    this.startListening();
  }

  /**
   * Démarrer l'écoute des nouveaux messages
   */
  startListening(): void {
    if (this.isListening) return;

    this.isListening = true;

    // Vérifier les nouveaux messages toutes les 5 secondes
    interval(this.CHECK_INTERVAL)
      .pipe(switchMap(() => this.checkForNewMessages()))
      .subscribe({
        next: newMessages => {
          if (newMessages.length > 0) {
            this.processNewMessages(newMessages);
          }
        },
        error: error => {
          console.error('Erreur lors de la vérification des messages:', error);
        },
      });
  }

  /**
   * Arrêter l'écoute des nouveaux messages
   */
  stopListening(): void {
    this.isListening = false;
  }

  /**
   * Vérifier s'il y a de nouveaux messages
   */
  private checkForNewMessages(): Observable<MessageNotification[]> {
    const params = {
      since: this.lastMessageCheck.toISOString(),
      page: '0',
      size: '50',
      sort: 'timestamp,desc',
    };

    return this.http.get<MessageNotification[]>('/api/messages/recent', { params }).pipe(
      tap(() => {
        this.lastMessageCheck = new Date();
      }),
      catchError(error => {
        console.error('Erreur lors de la récupération des messages récents:', error);
        // Retourner des messages de test en cas d'erreur
        return this.getMockRecentMessages();
      }),
    );
  }

  /**
   * Traiter les nouveaux messages et créer des notifications
   */
  private processNewMessages(messages: MessageNotification[]): void {
    messages.forEach(message => {
      // Créer une notification seulement si le message vient d'un client
      if (message.isFromClient) {
        this.createMessageNotification(message);
      }
    });
  }

  /**
   * Créer une notification pour un nouveau message
   * Note: Les notifications sont maintenant créées directement par le backend
   */
  private createMessageNotification(message: MessageNotification): void {
    // Les notifications sont créées directement par le backend dans TicketResource
    console.log('Message reçu (notification gérée par le backend):', message);
  }

  /**
   * Messages de fallback
   */
  private getMockRecentMessages(): Observable<MessageNotification[]> {
    return new Observable(observer => {
      observer.next([]);
      observer.complete();
    });
  }

  /**
   * Forcer la vérification des nouveaux messages
   */
  forceCheck(): void {
    this.checkForNewMessages().subscribe({
      next: messages => {
        if (messages.length > 0) {
          this.processNewMessages(messages);
        }
      },
      error: error => console.error('Erreur lors de la vérification forcée:', error),
    });
  }

  /**
   * Obtenir les messages récents
   */
  getRecentMessages(): Observable<MessageNotification[]> {
    return this.newMessages$.asObservable();
  }

  /**
   * Marquer un message comme traité
   */
  markMessageAsProcessed(messageId: number): void {
    // Logique pour marquer un message comme traité
    console.log('Message marqué comme traité:', messageId);
  }
}
