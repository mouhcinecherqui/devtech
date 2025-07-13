import { enableProdMode } from '@angular/core';
import { bootstrapApplication } from '@angular/platform-browser';
import { appConfig } from './app/app.config';
import AppComponent from './app/app.component';
import { environment } from './environments/environment';
import { StateStorageService } from './app/core/auth/state-storage.service';

// Handle JWT from OAuth2 redirect
(function handleOAuth2Jwt() {
  const url = new URL(window.location.href);
  const jwt = url.searchParams.get('jwt');
  if (jwt) {
    // Store JWT in sessionStorage (not rememberMe)
    const storage = new StateStorageService();
    storage.storeAuthenticationToken(jwt, false);
    // Remove jwt from URL
    url.searchParams.delete('jwt');
    window.history.replaceState({}, document.title, url.pathname + url.search);
  }
})();

// disable debug data on prod profile to improve performance
if (!environment.DEBUG_INFO_ENABLED) {
  enableProdMode();
}

bootstrapApplication(AppComponent, appConfig)
  // eslint-disable-next-line no-console
  .then(() => console.log('Application started'))
  .catch((err: unknown) => console.error(err));
