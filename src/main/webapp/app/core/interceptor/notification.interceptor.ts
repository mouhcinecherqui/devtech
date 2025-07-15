import { HttpEvent, HttpHandler, HttpInterceptor, HttpRequest, HttpResponse } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { Observable } from 'rxjs';
import { tap } from 'rxjs/operators';

import { AlertService } from 'app/core/util/alert.service';
import { HttpClient } from '@angular/common/http';

export interface Notification {
  id: number;
  userLogin: string;
  message: string;
  type: string;
  isRead: boolean;
  createdDate: string;
}

@Injectable({ providedIn: 'root' })
export class NotificationService {
  notifications = signal<Notification[]>([]);
  unreadCount = signal(0);

  constructor(private http: HttpClient) {}

  fetchNotifications() {
    this.http.get<Notification[]>('/api/notifications').subscribe(list => {
      // Force le signal en créant une nouvelle référence
      this.notifications.set([...list]);
      this.unreadCount.set(list.filter(n => !n.isRead).length);
      console.log('Notifications rafraîchies:', list);
    });
  }

  markAsRead(id: number) {
    return this.http.put(`/api/notifications/${id}/read`, {}).subscribe(() => {
      this.fetchNotifications();
    });
  }

  markAllAsRead() {
    return this.http.put('/api/notifications/read-all', {}).subscribe(() => {
      this.fetchNotifications();
    });
  }
}

@Injectable()
export class NotificationInterceptor implements HttpInterceptor {
  private readonly alertService = inject(AlertService);

  intercept(request: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    return next.handle(request).pipe(
      tap((event: HttpEvent<any>) => {
        if (event instanceof HttpResponse) {
          let alert: string | null = null;
          let alertParams: string | null = null;

          for (const headerKey of event.headers.keys()) {
            if (headerKey.toLowerCase().endsWith('app-alert')) {
              alert = event.headers.get(headerKey);
            } else if (headerKey.toLowerCase().endsWith('app-params')) {
              alertParams = decodeURIComponent(event.headers.get(headerKey)!.replace(/\+/g, ' '));
            }
          }

          if (alert) {
            this.alertService.addAlert({
              type: 'success',
              translationKey: alert,
              translationParams: { param: alertParams },
            });
          }
        }
      }),
    );
  }
}
