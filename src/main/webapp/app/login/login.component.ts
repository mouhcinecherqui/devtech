// Modification forcée pour déclencher la recompilation Angular des styles du composant login
import { AfterViewInit, Component, ElementRef, OnInit, inject, signal, viewChild } from '@angular/core';
import { FormControl, FormGroup, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';

import SharedModule from 'app/shared/shared.module';
import { LoginService } from 'app/login/login.service';
import { AccountService } from 'app/core/auth/account.service';
import { ClientsService } from 'app/admin/clients/clients.service';
import { catchError, of } from 'rxjs';

@Component({
  selector: 'jhi-login',
  imports: [SharedModule, FormsModule, ReactiveFormsModule, RouterModule],
  templateUrl: './login.component.html',
  styleUrl: './login.component.scss',
})
export default class LoginComponent implements OnInit {
  authenticationError = signal(false);
  // Supprimer clientLoginError car on utilise un seul message d'erreur

  loginForm = new FormGroup({
    usernameOrEmail: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    password: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    rememberMe: new FormControl(false, { nonNullable: true }),
  });

  // Supprimer clientLoginForm

  private readonly accountService = inject(AccountService);
  private readonly loginService = inject(LoginService);
  private readonly clientsService = inject(ClientsService);
  private readonly router = inject(Router);

  ngOnInit(): void {
    // Suppression de la redirection automatique vers /home
    // L'utilisateur peut accéder à /home même s'il est authentifié
  }

  login(): void {
    const { usernameOrEmail, password, rememberMe } = this.loginForm.getRawValue();
    // Tenter d'abord la connexion admin, puis client si échec
    this.loginService
      .login({ username: usernameOrEmail, password, rememberMe })
      .pipe(
        catchError(() => {
          console.log('Tentative client');
          // Si échec, tenter la connexion client
          return this.clientsService.login(usernameOrEmail, password).pipe(
            catchError(() => {
              this.authenticationError.set(true);
              return of(null);
            }),
          );
        }),
      )
      .subscribe(result => {
        if (result) {
          this.authenticationError.set(false);
          // Rediriger selon le rôle
          const authorities = (result as any).authorities || [];
          if (authorities.includes('ROLE_ADMIN')) {
            this.router.navigate(['/admin-dashboard']);
          } else if (authorities.includes('ROLE_MANAGER')) {
            this.router.navigate(['/manager-dashboard']);
          } else if (authorities.includes('ROLE_CLIENT')) {
            this.router.navigate(['/client-dashboard']);
          } else if (authorities.includes('ROLE_USER')) {
            this.router.navigate(['/user-dashboard']);
          } else {
            this.router.navigate(['/home']);
          }
        }
      });
  }
}
