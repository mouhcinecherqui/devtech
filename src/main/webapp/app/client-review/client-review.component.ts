import { Component, inject, OnInit, signal, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';

import SharedModule from 'app/shared/shared.module';
import { ClientReviewService, ClientReviewDTO } from '../core/services/client-review.service';

@Component({
  selector: 'jhi-client-review',
  templateUrl: './client-review.component.html',
  styleUrls: ['./client-review.component.scss'],
  standalone: true,
  imports: [CommonModule, SharedModule, FormsModule],
})
export class ClientReviewComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly clientReviewService = inject(ClientReviewService);
  private readonly translateService = inject(TranslateService);

  @Input() ticketIdInput?: number;
  @Input() embedded = false;
  @Output() submitted = new EventEmitter<void>();

  ticketId = signal<number | null>(null);
  clientName = signal<string>('');
  rating = signal<number>(0);
  comment = signal<string>('');
  isSubmitting = signal<boolean>(false);
  error = signal<string | null>(null);
  success = signal<string | null>(null);

  // Étoiles pour l'évaluation
  stars = [1, 2, 3, 4, 5];
  hoveredStar = 0;

  ngOnInit(): void {
    if (this.ticketIdInput) {
      this.ticketId.set(this.ticketIdInput);
    } else {
      this.route.params.subscribe(params => {
        const id = params['id'];
        if (id) {
          this.ticketId.set(Number(id));
        }
      });
    }
  }

  // Gestion des étoiles
  onStarClick(star: number): void {
    this.rating.set(star);
  }

  onStarHover(star: number): void {
    this.hoveredStar = star;
  }

  onStarLeave(): void {
    this.hoveredStar = 0;
  }

  isStarActive(star: number): boolean {
    return star <= (this.hoveredStar || this.rating());
  }

  // Soumission de l'avis
  submitReview(): void {
    if (!this.validateForm()) {
      return;
    }

    this.isSubmitting.set(true);
    this.error.set(null);

    const nowIso = new Date().toISOString();

    const review: ClientReviewDTO = {
      ticketId: this.ticketId()!,
      clientName: this.clientName(),
      rating: this.rating(),
      comment: this.comment(),
      isApproved: true,
      createdDate: nowIso,
      updatedDate: nowIso,
    };

    this.clientReviewService.create(review).subscribe({
      next: () => {
        this.success.set('Votre avis a été soumis avec succès !');
        this.isSubmitting.set(false);
        this.submitted.emit();

        if (!this.embedded) {
          // Rediriger après 2 secondes (mode page)
          setTimeout(() => {
            this.router.navigate(['/client-tickets']);
          }, 2000);
        }
      },
      error: err => {
        this.error.set('Erreur lors de la soumission de votre avis. Veuillez réessayer.');
        this.isSubmitting.set(false);
        console.error('Erreur soumission avis:', err);
      },
    });
  }

  // Validation du formulaire (avec messages) pour la soumission uniquement
  private validateForm(): boolean {
    const name = (this.clientName() ?? '').trim();
    const rating = this.rating() ?? 0;
    const comment = (this.comment() ?? '').trim();

    if (!name) {
      this.error.set('Veuillez saisir votre nom.');
      return false;
    }
    if (rating < 1) {
      this.error.set('Veuillez donner une note en cliquant sur les étoiles.');
      return false;
    }
    if (comment.length < 10) {
      this.error.set("Veuillez saisir un commentaire d'au moins 10 caractères.");
      return false;
    }
    this.error.set(null);
    return true;
  }

  // Validation "pure" pour le template (ne modifie aucun signal)
  canSubmit(): boolean {
    const name = (this.clientName() ?? '').trim();
    const rating = this.rating() ?? 0;
    const comment = (this.comment() ?? '').trim();
    return !!name && rating >= 1 && comment.length >= 10;
  }

  // Navigation
  goBack(): void {
    if (!this.embedded) {
      this.router.navigate(['/client-tickets']);
    }
  }

  // Traduction
  getRatingText(): string {
    const rating = this.rating();
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
        return 'Sélectionnez une note';
    }
  }
}
