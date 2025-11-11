import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TranslateService } from '@ngx-translate/core';

import SharedModule from 'app/shared/shared.module';
import { ClientReviewService, ClientReviewDTO, ReviewStats } from '../core/services/client-review.service';

@Component({
  selector: 'jhi-client-reviews-display',
  templateUrl: './client-reviews-display.component.html',
  styleUrls: ['./client-reviews-display.component.scss'],
  standalone: true,
  imports: [CommonModule, SharedModule],
})
export class ClientReviewsDisplayComponent implements OnInit {
  private readonly clientReviewService = inject(ClientReviewService);
  private readonly translateService = inject(TranslateService);

  // Make Math available in template
  protected readonly Math = Math;

  reviews = signal<ClientReviewDTO[]>([]);
  stats = signal<ReviewStats | null>(null);
  isLoading = signal<boolean>(true);
  error = signal<string | null>(null);

  ngOnInit(): void {
    // Charger les données pour tous les utilisateurs (maintenant accessibles publiquement)
    this.loadReviews();
    this.loadStats();
  }

  loadReviews(): void {
    this.isLoading.set(true);
    this.error.set(null);

    this.clientReviewService.getPublicApprovedReviews().subscribe({
      next: reviews => {
        this.reviews.set(reviews.slice(0, 6)); // Afficher seulement les 6 premiers avis
        this.isLoading.set(false);
      },
      error: err => {
        this.error.set('Erreur lors du chargement des avis');
        this.isLoading.set(false);
        console.error('Erreur chargement avis:', err);
      },
    });
  }

  loadStats(): void {
    this.clientReviewService.getPublicReviewStats().subscribe({
      next: stats => {
        this.stats.set(stats);
      },
      error(err) {
        console.error('Erreur chargement stats:', err);
      },
    });
  }

  // Générer les étoiles pour l'affichage
  getStars(rating: number): boolean[] {
    return Array.from({ length: 5 }, (_, i) => i < rating);
  }

  // Formater la date
  formatDate(dateString: string): string {
    const date = new Date(dateString);
    return date.toLocaleDateString('fr-FR', {
      day: 'numeric',
      month: 'long',
      year: 'numeric',
    });
  }

  // Obtenir la couleur de l'étoile
  getStarColor(index: number, rating: number): string {
    return index < rating ? '#ffc107' : '#ddd';
  }

  // Obtenir le texte de la note
  getRatingText(rating: number): string {
    switch (rating) {
      case 1:
        return 'Très mauvais';
      case 2:
        return 'Mauvais';
      case 3:
        return 'Moyen';
      case 4:
        return 'Bon';
      case 5:
        return 'Excellent';
      default:
        return '';
    }
  }

  // Tronquer le commentaire si trop long
  truncateComment(comment: string, maxLength = 150): string {
    if (comment.length <= maxLength) {
      return comment;
    }
    return comment.substring(0, maxLength) + '...';
  }

  // Formater la note moyenne de manière sécurisée
  formatAverageRating(rating: number | undefined | null): string {
    if (rating === undefined || rating === null) {
      return '0.0';
    }
    return rating.toFixed(1);
  }
}
