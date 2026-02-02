import { Injectable } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { StateStorageService } from './state-storage.service';
import { AccountService } from './account.service';
import { take } from 'rxjs/operators';

@Injectable({
  providedIn: 'root',
})
export class OAuth2Service {
  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private http: HttpClient,
    private stateStorageService: StateStorageService,
    private accountService: AccountService,
  ) {}

  checkOAuth2Success(): void {
    this.route.queryParams.pipe(take(1)).subscribe(params => {
      if (params['oauth2'] === 'success' && params['token']) {
        const token = decodeURIComponent(params['token']);
        this.stateStorageService.storeAuthenticationToken(token, true);

        // Forcer un rechargement du compte (évite le cache null) puis rediriger
        this.accountService
          .identity(true)
          .pipe(take(1))
          .subscribe(account => {
            if (account?.authorities?.length) {
              const authorities = account.authorities as string[];
              if (authorities.includes('ROLE_CLIENT')) {
                this.router.navigate(['/client-dashboard'], { replaceUrl: true });
              } else if (authorities.includes('ROLE_ADMIN')) {
                this.router.navigate(['/admin-dashboard'], { replaceUrl: true });
              } else if (authorities.includes('ROLE_MANAGER')) {
                this.router.navigate(['/manager-dashboard'], { replaceUrl: true });
              } else if (authorities.includes('ROLE_USER')) {
                this.router.navigate(['/user-dashboard'], { replaceUrl: true });
              } else {
                this.router.navigate(['/home'], { replaceUrl: true });
              }
            } else {
              // Pas de compte (erreur) : nettoyer l’URL et rester sur login
              this.router.navigate([], {
                relativeTo: this.route,
                queryParams: {},
                replaceUrl: true,
              });
            }
          });
      } else if (params['error'] === 'oauth2_failed') {
        console.error('OAuth2 authentication failed');
      }
    });
  }
}
