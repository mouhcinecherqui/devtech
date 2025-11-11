import { Injectable } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { StateStorageService } from './state-storage.service';
import { AccountService } from './account.service'; // Import AccountService
import { filter, take } from 'rxjs/operators';

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
    this.route.queryParams.subscribe(params => {
      if (params['oauth2'] === 'success' && params['token']) {
        const token = decodeURIComponent(params['token']);
        this.stateStorageService.storeAuthenticationToken(token, true);

        // Clean the URL only; let login.component's identity subscription decide navigation
        this.router.navigate([], {
          relativeTo: this.route,
          queryParams: {},
          replaceUrl: true,
        });

        console.warn('OAuth2 Success: token stored, waiting for identity to redirect', {
          name: params['name'],
          email: params['email'],
          token: token.substring(0, 20) + '...',
        });
      } else if (params['error'] === 'oauth2_failed') {
        console.error('OAuth2 authentication failed');
      }
    });
  }
}
