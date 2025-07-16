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
    // if already authenticated then navigate to home page
    this.accountService.identity().subscribe(() => {
      if (this.accountService.isAuthenticated()) {
        this.router.navigate(['']);
      }
    });
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
          // Si c'est un compte admin, rediriger vers home, sinon vers dashboard client
          if ((result as any).authorities) {
            if (!this.router.getCurrentNavigation()) {
              this.router.navigate(['']);
            }
          } else {
            this.router.navigate(['/user-dashboard']);
          }
        }
      });
  }
}
