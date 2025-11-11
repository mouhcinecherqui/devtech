import { Component, inject, signal, computed, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { NotificationService, Notification } from '../../services/notification.service';

@Component({
  selector: 'jhi-notification-bell',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './notification-bell.component.html',
  styleUrls: ['./notification-bell.component.scss'],
})
export class NotificationBellComponent implements OnInit, OnDestroy {
  private readonly notificationService = inject(NotificationService);

  // Signals
  public readonly notifications = this.notificationService.notifications$;
  public readonly unreadCount = this.notificationService.unreadCount$;
  public readonly isLoading = this.notificationService.isLoading$;

  // État local
  public readonly isOpen = signal(false);
  public readonly selectedNotification = signal<Notification | null>(null);

  // Computed
  public readonly recentNotifications = computed(
    () => this.notifications().slice(0, 5), // Afficher les 5 plus récentes
  );

  public readonly hasUnread = computed(() => this.unreadCount() > 0);

  ngOnInit(): void {
    // Charger les notifications au démarrage
    this.notificationService.fetchNotifications().subscribe();
  }

  ngOnDestroy(): void {
    // Nettoyage si nécessaire
  }

  /**
   * Basculer l'ouverture du dropdown
   */
  toggleDropdown(): void {
    this.isOpen.update(open => !open);

    // Charger les notifications quand on ouvre le dropdown
    if (this.isOpen()) {
      this.notificationService.fetchNotifications().subscribe();
    }
  }

  /**
   * Fermer le dropdown
   */
  closeDropdown(): void {
    this.isOpen.set(false);
  }

  /**
   * Marquer une notification comme lue
   */
  markAsRead(notification: Notification): void {
    if (!notification.read) {
      this.notificationService.markAsRead(notification.id).subscribe(() => {
        // Recharger les notifications après marquage
        this.notificationService.fetchNotifications().subscribe();
      });
    }
  }

  /**
   * Marquer toutes les notifications comme lues
   */
  markAllAsRead(): void {
    this.notificationService.markAllAsRead().subscribe(() => {
      // Recharger les notifications après marquage
      this.notificationService.fetchNotifications().subscribe();
    });
  }

  /**
   * Obtenir l'icône basée sur le type de notification
   */
  getNotificationIcon(type: string): string {
    switch (type) {
      case 'TICKET_CREATED':
        return 'ticket';
      case 'TICKET_UPDATED':
        return 'edit';
      case 'MESSAGE_RECEIVED':
        return 'message';
      case 'PAYMENT_RECEIVED':
      case 'PAYMENT_CONFIRMED':
        return 'payment';
      default:
        return 'info';
    }
  }

  /**
   * Obtenir la couleur basée sur le type de notification
   */
  getNotificationColor(type: string): string {
    switch (type) {
      case 'TICKET_CREATED':
        return 'success';
      case 'TICKET_UPDATED':
        return 'info';
      case 'MESSAGE_RECEIVED':
        return 'primary';
      case 'PAYMENT_RECEIVED':
      case 'PAYMENT_CONFIRMED':
        return 'success';
      default:
        return 'secondary';
    }
  }

  /**
   * Formater la date relative
   */
  getRelativeTime(timestamp: string): string {
    const now = new Date();
    const notificationDate = new Date(timestamp);
    const diffInSeconds = Math.floor((now.getTime() - notificationDate.getTime()) / 1000);

    if (diffInSeconds < 60) {
      return "À l'instant";
    } else if (diffInSeconds < 3600) {
      const minutes = Math.floor(diffInSeconds / 60);
      return `Il y a ${minutes} min`;
    } else if (diffInSeconds < 86400) {
      const hours = Math.floor(diffInSeconds / 3600);
      return `Il y a ${hours}h`;
    } else {
      const days = Math.floor(diffInSeconds / 86400);
      return `Il y a ${days} jour${days > 1 ? 's' : ''}`;
    }
  }
}
