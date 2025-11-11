import { Component, OnInit, inject, signal, HostListener } from '@angular/core';
import { Router, RouterModule } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';
import { FormsModule } from '@angular/forms';

import { StateStorageService } from 'app/core/auth/state-storage.service';
import SharedModule from 'app/shared/shared.module';
import HasAnyAuthorityDirective from 'app/shared/auth/has-any-authority.directive';
import { LANGUAGES } from 'app/config/language.constants';
import { AccountService } from 'app/core/auth/account.service';
import { LoginService } from 'app/login/login.service';
import { ProfileService } from 'app/layouts/profiles/profile.service';
import { EntityNavbarItems } from 'app/entities/entity-navbar-items';
import { environment } from 'environments/environment';
import NavbarItem from './navbar-item.model';
import { NotificationBellComponent } from 'app/core/components/notification-bell/notification-bell.component';

@Component({
  selector: 'jhi-navbar',
  templateUrl: './navbar.component.html',
  styleUrl: './navbar.component.scss',
  imports: [RouterModule, SharedModule, HasAnyAuthorityDirective, FormsModule, NotificationBellComponent],
})
export default class NavbarComponent implements OnInit {
  inProduction?: boolean;
  isNavbarCollapsed = signal(true);
  languages = LANGUAGES;
  openAPIEnabled?: boolean;
  version = '';
  account = inject(AccountService).trackCurrentAccount();
  entitiesNavbarItems: NavbarItem[] = [];
  isRTL = signal(false);
  toggleUserMenu = false;

  public translateService = inject(TranslateService);
  private readonly loginService = inject(LoginService);
  private readonly stateStorageService = inject(StateStorageService);
  private readonly profileService = inject(ProfileService);
  private readonly router = inject(Router);

  constructor() {
    const { VERSION } = environment;
    if (VERSION) {
      this.version = VERSION.toLowerCase().startsWith('v') ? VERSION : `v${VERSION}`;
    }
  }

  ngOnInit(): void {
    this.entitiesNavbarItems = EntityNavbarItems;
    this.profileService.getProfileInfo().subscribe(profileInfo => {
      this.inProduction = profileInfo.inProduction;
      this.openAPIEnabled = profileInfo.openAPIEnabled;
    });

    // Initialiser la direction RTL basée sur la langue actuelle
    this.updateRTLLayout(this.translateService.currentLang || 'fr');
  }

  changeLanguage(languageKey: string): void {
    this.stateStorageService.storeLocale(languageKey);
    this.translateService.use(languageKey);
    this.updateRTLLayout(languageKey);
  }

  private updateRTLLayout(langCode: string): void {
    const isRTL = langCode === 'ar';
    this.isRTL.set(isRTL);

    // Appliquer la direction RTL au document
    if (isRTL) {
      document.documentElement.dir = 'rtl';
      document.documentElement.lang = 'ar';
      document.body.classList.add('rtl');
    } else {
      document.documentElement.dir = 'ltr';
      document.documentElement.lang = langCode;
      document.body.classList.remove('rtl');
    }
  }

  collapseNavbar(): void {
    this.isNavbarCollapsed.set(true);
  }

  login(): void {
    this.router.navigate(['/login']);
  }

  logout(): void {
    this.collapseNavbar();
    this.loginService.logout();
    this.router.navigate(['']);
  }

  toggleNavbar(): void {
    this.isNavbarCollapsed.update(isNavbarCollapsed => !isNavbarCollapsed);
  }

  getLogoLink(): string {
    const currentAccount = this.account();

    // Si l'utilisateur n'est pas authentifié, rediriger vers home
    if (!currentAccount) {
      return '/home';
    }

    // Rediriger vers le dashboard approprié selon le rôle
    const authorities = currentAccount.authorities ?? [];

    if (authorities.includes('ROLE_ADMIN')) {
      return '/admin-dashboard';
    } else if (authorities.includes('ROLE_MANAGER')) {
      return '/manager-dashboard';
    } else if (authorities.includes('ROLE_CLIENT')) {
      return '/client-dashboard';
    } else if (authorities.includes('ROLE_USER')) {
      return '/user-dashboard';
    }

    // Par défaut, rediriger vers home si aucun rôle spécifique
    return '/home';
  }

  goToAccount(): void {
    this.router.navigate(['/account/settings']);
  }
}
