import { Component, Input, Output, EventEmitter, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AutoRefreshService } from '../../../core/services/auto-refresh.service';

@Component({
  selector: 'jhi-refresh-button',
  standalone: true,
  imports: [CommonModule],
  template: `
    <button
      class="refresh-btn"
      [class.loading]="loading"
      [disabled]="loading"
      (click)="onRefresh()"
      [title]="loading ? 'Actualisation en cours...' : 'Actualiser les données'"
    >
      <svg class="refresh-icon" [class.spinning]="loading" width="16" height="16" viewBox="0 0 24 24" fill="none">
        <path d="M1 4V10H7" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" />
        <path d="M23 20V14H17" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" />
        <path
          d="M20.49 9A9 9 0 0 0 5.64 5.64L1 10M23 14L18.36 18.36A9 9 0 0 1 3.51 15"
          stroke="currentColor"
          stroke-width="2"
          stroke-linecap="round"
          stroke-linejoin="round"
        />
      </svg>
      <span class="refresh-text">{{ loading ? 'Actualisation...' : 'Actualiser' }}</span>
    </button>
  `,
  styles: [
    `
      .refresh-btn {
        display: inline-flex;
        align-items: center;
        gap: 0.5rem;
        padding: 0.5rem 1rem;
        background: rgba(255, 255, 255, 0.1);
        border: 1px solid rgba(255, 255, 255, 0.2);
        border-radius: 8px;
        color: #ffffff;
        font-size: 0.875rem;
        font-weight: 500;
        cursor: pointer;
        transition: all 0.2s ease;
        backdrop-filter: blur(10px);
      }

      .refresh-btn:hover:not(:disabled) {
        background: rgba(255, 255, 255, 0.2);
        border-color: rgba(255, 255, 255, 0.3);
        transform: translateY(-1px);
      }

      .refresh-btn:disabled {
        opacity: 0.6;
        cursor: not-allowed;
        transform: none;
      }

      .refresh-btn.loading {
        background: rgba(102, 126, 234, 0.2);
        border-color: rgba(102, 126, 234, 0.4);
      }

      .refresh-icon {
        width: 16px;
        height: 16px;
        transition: transform 0.3s ease;
      }

      .refresh-icon.spinning {
        animation: spin 1s linear infinite;
      }

      .refresh-text {
        white-space: nowrap;
      }

      @keyframes spin {
        from {
          transform: rotate(0deg);
        }
        to {
          transform: rotate(360deg);
        }
      }

      /* Version compacte */
      .refresh-btn.compact {
        padding: 0.375rem 0.75rem;
        font-size: 0.8rem;
      }

      .refresh-btn.compact .refresh-text {
        display: none;
      }

      .refresh-btn.compact .refresh-icon {
        width: 18px;
        height: 18px;
      }
    `,
  ],
})
export class RefreshButtonComponent {
  @Input() loading = false;
  @Input() compact = false;
  @Output() refresh = new EventEmitter<void>();

  private readonly autoRefreshService = inject(AutoRefreshService);

  onRefresh(): void {
    if (!this.loading) {
      this.refresh.emit();
      this.autoRefreshService.forceRefresh();
    }
  }
}
