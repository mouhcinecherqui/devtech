import { Injectable, inject, ChangeDetectorRef } from '@angular/core';
import { BehaviorSubject, Observable, timer, Subscription } from 'rxjs';
import { takeUntil, switchMap } from 'rxjs/operators';

@Injectable({
  providedIn: 'root',
})
export class AutoRefreshService {
  private refreshInterval$ = new BehaviorSubject<number>(30000); // 30 secondes par défaut
  private isEnabled$ = new BehaviorSubject<boolean>(true);
  private destroy$ = new BehaviorSubject<boolean>(false);

  // Observables publics
  public refreshTrigger$: Observable<number>;
  public isEnabled: Observable<boolean>;

  constructor() {
    // Créer un timer qui se déclenche selon l'intervalle configuré
    this.refreshTrigger$ = this.isEnabled$.pipe(
      switchMap(enabled =>
        enabled ? timer(0, this.refreshInterval$.value).pipe(takeUntil(this.destroy$)) : timer(0).pipe(takeUntil(this.destroy$)),
      ),
    );

    this.isEnabled = this.isEnabled$.asObservable();
  }

  /**
   * Configure l'intervalle de rafraîchissement automatique
   * @param intervalMs Intervalle en millisecondes
   */
  setRefreshInterval(intervalMs: number): void {
    this.refreshInterval$.next(intervalMs);
  }

  /**
   * Active ou désactive le rafraîchissement automatique
   * @param enabled true pour activer, false pour désactiver
   */
  setEnabled(enabled: boolean): void {
    this.isEnabled$.next(enabled);
  }

  /**
   * Force un rafraîchissement immédiat
   */
  forceRefresh(): void {
    this.refreshTrigger$.subscribe();
  }

  /**
   * Détruit le service et arrête tous les timers
   */
  destroy(): void {
    this.destroy$.next(true);
  }

  /**
   * Méthode utilitaire pour forcer la détection des changements sur un composant
   * @param cdr ChangeDetectorRef du composant
   */
  static forceChangeDetection(cdr: ChangeDetectorRef): void {
    try {
      cdr.detectChanges();
    } catch (error) {
      console.warn('Erreur lors de la détection des changements:', error);
    }
  }

  /**
   * Méthode utilitaire pour marquer un composant pour vérification
   * @param cdr ChangeDetectorRef du composant
   */
  static markForCheck(cdr: ChangeDetectorRef): void {
    try {
      cdr.markForCheck();
    } catch (error) {
      console.warn('Erreur lors du marquage pour vérification:', error);
    }
  }
}
