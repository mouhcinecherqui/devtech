import { Component, Input, Output, EventEmitter, inject, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { NotificationService, Notification, NotificationStats } from '../../../core/services/notification.service';

@Component({
  selector: 'jhi-notification-badge',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="notification-container" (click)="toggleDropdown()">
      <!-- Badge de notification -->
      <div class="notification-badge" [class.has-notifications]="unreadCount() > 0">
        <svg class="notification-icon" width="20" height="20" viewBox="0 0 24 24" fill="none">
          <path
            d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9"
            stroke="currentColor"
            stroke-width="2"
            stroke-linecap="round"
            stroke-linejoin="round"
          />
          <path d="M13.73 21a2 2 0 0 1-3.46 0" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" />
        </svg>

        <!-- Compteur de notifications non lues -->
        <span *ngIf="unreadCount() > 0" class="notification-count" [class.high]="unreadCount() > 9">
          {{ unreadCount() > 99 ? '99+' : unreadCount() }}
        </span>
      </div>

      <!-- Dropdown des notifications -->
      <div class="notification-dropdown" [class.show]="showDropdown" (click)="$event.stopPropagation()">
        <!-- Header -->
        <div class="dropdown-header">
          <h4>Notifications</h4>
          <div class="header-actions">
            <button *ngIf="stats().unread > 0" class="mark-all-read-btn" (click)="markAllAsRead()" [disabled]="isLoading()">
              Tout marquer comme lu
            </button>
            <button class="refresh-btn" (click)="refreshNotifications()" [disabled]="isLoading()">
              <svg class="refresh-icon" [class.spinning]="isLoading()" width="16" height="16" viewBox="0 0 24 24" fill="none">
                <path
                  d="M1 4V10H7M23 20V14H17M20.49 9A9 9 0 0 0 5.64 5.64L1 10M23 14L18.36 18.36A9 9 0 0 1 3.51 15"
                  stroke="currentColor"
                  stroke-width="2"
                  stroke-linecap="round"
                  stroke-linejoin="round"
                />
              </svg>
            </button>
          </div>
        </div>

        <!-- Liste des notifications -->
        <div class="notifications-list">
          <div
            *ngFor="let notification of notifications(); trackBy: trackByNotificationId"
            class="notification-item"
            [class.unread]="!notification.read"
            [class]="'type-' + notification.type"
          >
            <div class="notification-content">
              <div class="notification-header">
                <h5 class="notification-title">{{ notification.title }}</h5>
                <span class="notification-time">{{ formatTime(notification.timestamp) }}</span>
              </div>
              <p class="notification-message">{{ notification.message }}</p>
            </div>
            <div class="notification-actions">
              <button
                *ngIf="!notification.read"
                class="mark-read-btn"
                (click)="markAsRead(notification.id); $event.stopPropagation()"
                title="Marquer comme lu"
              >
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none">
                  <path d="M20 6L9 17L4 12" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" />
                </svg>
              </button>
              <button class="delete-btn" (click)="deleteNotification(notification.id); $event.stopPropagation()" title="Supprimer">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none">
                  <path
                    d="M3 6H5H21M8 6V4A2 2 0 0 1 10 2H14A2 2 0 0 1 16 4V6M19 6V20A2 2 0 0 1 17 22H7A2 2 0 0 1 5 20V6H19Z"
                    stroke="currentColor"
                    stroke-width="2"
                    stroke-linecap="round"
                    stroke-linejoin="round"
                  />
                </svg>
              </button>
            </div>
          </div>

          <!-- État vide -->
          <div *ngIf="notifications().length === 0" class="empty-state">
            <svg width="48" height="48" viewBox="0 0 24 24" fill="none" opacity="0.5">
              <path
                d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9"
                stroke="currentColor"
                stroke-width="2"
                stroke-linecap="round"
                stroke-linejoin="round"
              />
            </svg>
            <p>Aucune notification</p>
          </div>
        </div>

        <!-- Footer avec statistiques -->
        <div class="dropdown-footer" *ngIf="notifications().length > 0">
          <div class="notification-stats">
            <span class="stat-item">
              <strong>{{ stats().total }}</strong> total
            </span>
            <span class="stat-item">
              <strong>{{ stats().unread }}</strong> non lues
            </span>
          </div>
        </div>
      </div>
    </div>
  `,
  styles: [
    `
      .notification-container {
        position: relative;
        cursor: pointer;
      }

      .notification-badge {
        position: relative;
        display: flex;
        align-items: center;
        justify-content: center;
        width: 40px;
        height: 40px;
        background: rgba(255, 255, 255, 0.1);
        border-radius: 50%;
        transition: all 0.3s ease;
        border: 1px solid rgba(255, 255, 255, 0.2);
      }

      .notification-badge:hover {
        background: rgba(255, 255, 255, 0.2);
        transform: scale(1.05);
      }

      .notification-badge.has-notifications {
        background: rgba(239, 68, 68, 0.2);
        border-color: rgba(239, 68, 68, 0.4);
      }

      .notification-icon {
        color: #ffffff;
        transition: color 0.3s ease;
      }

      .notification-badge.has-notifications .notification-icon {
        color: #fca5a5;
      }

      .notification-count {
        position: absolute;
        top: -5px;
        right: -5px;
        background: #ef4444;
        color: white;
        border-radius: 50%;
        min-width: 18px;
        height: 18px;
        display: flex;
        align-items: center;
        justify-content: center;
        font-size: 11px;
        font-weight: 600;
        border: 2px solid #ffffff;
        animation: pulse 2s infinite;
      }

      .notification-count.high {
        background: #dc2626;
        animation: pulse-fast 1s infinite;
      }

      .notification-dropdown {
        position: absolute;
        top: 50px;
        right: 0;
        width: 400px;
        max-height: 500px;
        background: rgba(255, 255, 255, 0.95);
        backdrop-filter: blur(20px);
        border-radius: 12px;
        border: 1px solid rgba(255, 255, 255, 0.2);
        box-shadow:
          0 20px 25px -5px rgba(0, 0, 0, 0.1),
          0 10px 10px -5px rgba(0, 0, 0, 0.04);
        opacity: 0;
        visibility: hidden;
        transform: translateY(-10px);
        transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
        z-index: 1000;
      }

      .notification-dropdown.show {
        opacity: 1;
        visibility: visible;
        transform: translateY(0);
      }

      .dropdown-header {
        display: flex;
        align-items: center;
        justify-content: space-between;
        padding: 1rem;
        border-bottom: 1px solid rgba(0, 0, 0, 0.1);
      }

      .dropdown-header h4 {
        margin: 0;
        font-size: 1.1rem;
        font-weight: 600;
        color: #1f2937;
      }

      .header-actions {
        display: flex;
        gap: 0.5rem;
      }

      .mark-all-read-btn,
      .refresh-btn {
        padding: 0.25rem 0.5rem;
        font-size: 0.75rem;
        border: 1px solid rgba(0, 0, 0, 0.1);
        border-radius: 6px;
        background: white;
        color: #6b7280;
        cursor: pointer;
        transition: all 0.2s ease;
      }

      .mark-all-read-btn:hover,
      .refresh-btn:hover {
        background: #f3f4f6;
        color: #374151;
      }

      .refresh-icon {
        transition: transform 0.3s ease;
      }

      .refresh-icon.spinning {
        animation: spin 1s linear infinite;
      }

      .notifications-list {
        max-height: 350px;
        overflow-y: auto;
      }

      .notification-item {
        display: flex;
        align-items: flex-start;
        gap: 0.75rem;
        padding: 1rem;
        border-bottom: 1px solid rgba(0, 0, 0, 0.05);
        transition: background-color 0.2s ease;
        position: relative;
      }

      .notification-item.unread {
        background: rgba(59, 130, 246, 0.05);
        border-left: 3px solid #3b82f6;
      }

      .notification-item.type-success {
        border-left-color: #10b981;
      }

      .notification-item.type-warning {
        border-left-color: #f59e0b;
      }

      .notification-item.type-error {
        border-left-color: #ef4444;
      }

      .notification-content {
        flex: 1;
        min-width: 0;
      }

      .notification-header {
        display: flex;
        align-items: center;
        justify-content: space-between;
        margin-bottom: 0.25rem;
      }

      .notification-title {
        margin: 0;
        font-size: 0.875rem;
        font-weight: 600;
        color: #1f2937;
      }

      .notification-time {
        font-size: 0.75rem;
        color: #6b7280;
        white-space: nowrap;
      }

      .notification-message {
        margin: 0;
        font-size: 0.8rem;
        color: #4b5563;
        line-height: 1.4;
      }

      .notification-actions {
        display: flex;
        flex-direction: column;
        gap: 0.25rem;
        opacity: 0;
        transition: opacity 0.2s ease;
      }

      .notification-item:hover .notification-actions {
        opacity: 1;
      }

      .mark-read-btn,
      .delete-btn {
        width: 24px;
        height: 24px;
        border: none;
        border-radius: 4px;
        background: rgba(0, 0, 0, 0.05);
        color: #6b7280;
        cursor: pointer;
        display: flex;
        align-items: center;
        justify-content: center;
        transition: all 0.2s ease;
      }

      .mark-read-btn:hover {
        background: #10b981;
        color: white;
      }

      .delete-btn:hover {
        background: #ef4444;
        color: white;
      }

      .empty-state {
        display: flex;
        flex-direction: column;
        align-items: center;
        justify-content: center;
        padding: 2rem;
        color: #6b7280;
      }

      .empty-state p {
        margin: 0.5rem 0 0 0;
        font-size: 0.875rem;
      }

      .dropdown-footer {
        padding: 0.75rem 1rem;
        border-top: 1px solid rgba(0, 0, 0, 0.1);
        background: rgba(0, 0, 0, 0.02);
      }

      .notification-stats {
        display: flex;
        justify-content: space-between;
        font-size: 0.75rem;
        color: #6b7280;
      }

      .stat-item strong {
        color: #374151;
      }

      @keyframes pulse {
        0%,
        100% {
          opacity: 1;
        }
        50% {
          opacity: 0.5;
        }
      }

      @keyframes pulse-fast {
        0%,
        100% {
          opacity: 1;
        }
        50% {
          opacity: 0.3;
        }
      }

      @keyframes spin {
        from {
          transform: rotate(0deg);
        }
        to {
          transform: rotate(360deg);
        }
      }

      /* Responsive */
      @media (max-width: 768px) {
        .notification-dropdown {
          width: 320px;
          right: -50px;
        }
      }
    `,
  ],
})
export class NotificationBadgeComponent {
  @Input() showDropdown = false;
  @Output() dropdownToggle = new EventEmitter<boolean>();

  private readonly notificationService = inject(NotificationService);
  private readonly router = inject(Router);

  // Signals
  notifications = this.notificationService.notifications$;
  isLoading = this.notificationService.isLoading$;
  unreadCount = this.notificationService.unreadCount$;
  stats = this.notificationService.notificationStats$;

  trackByNotificationId(index: number, notification: Notification): number {
    return notification.id;
  }

  toggleDropdown(): void {
    this.showDropdown = !this.showDropdown;
    this.dropdownToggle.emit(this.showDropdown);
  }

  markAsRead(notificationId: number): void {
    this.notificationService.markAsRead(notificationId).subscribe({
      error: error => console.error('Erreur lors du marquage de la notification:', error),
    });
  }

  markAllAsRead(): void {
    this.notificationService.markAllAsRead().subscribe({
      error: error => console.error('Erreur lors du marquage de toutes les notifications:', error),
    });
  }

  deleteNotification(notificationId: number): void {
    this.notificationService.deleteNotification(notificationId).subscribe({
      error: error => console.error('Erreur lors de la suppression de la notification:', error),
    });
  }

  refreshNotifications(): void {
    this.notificationService.forceRefresh();
  }

  // Méthode handleNotificationClick supprimée - les notifications ne sont plus cliquables

  formatTime(timestamp: string): string {
    const date = new Date(timestamp);
    const now = new Date();
    const diff = now.getTime() - date.getTime();
    const minutes = Math.floor(diff / 60000);
    const hours = Math.floor(diff / 3600000);
    const days = Math.floor(diff / 86400000);

    if (minutes < 1) return "À l'instant";
    if (minutes < 60) return `Il y a ${minutes}min`;
    if (hours < 24) return `Il y a ${hours}h`;
    if (days < 7) return `Il y a ${days}j`;

    return date.toLocaleDateString('fr-FR', {
      day: 'numeric',
      month: 'short',
    });
  }
}
