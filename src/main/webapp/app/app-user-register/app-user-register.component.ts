import { Component } from '@angular/core';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import SharedModule from '../shared/shared.module';

@Component({
  selector: 'jhi-user-register',
  standalone: true,
  imports: [CommonModule, SharedModule, ReactiveFormsModule, RouterModule],
  templateUrl: './app-user-register.component.html',
  styleUrls: ['./app-user-register.component.scss'],
})
export class AppUserRegisterComponent {
  form: FormGroup;
  submitted = false;
  success = false;
  error = false;

  constructor(
    private fb: FormBuilder,
    private http: HttpClient,
  ) {
    this.form = this.fb.group({
      firstName: ['', Validators.required],
      lastName: ['', Validators.required],
      email: ['', [Validators.required, Validators.email]],
      phone: ['', Validators.required],
      type: ['client'],
    });
  }

  submit(): void {
    if (this.form.valid) {
      this.submitted = true;
      const formValue = this.form.value;
      // Generate a random password for the client
      const randomPassword = Math.random().toString(36).slice(-8) + Math.random().toString(36).slice(-4);
      const payload = {
        login: formValue.email, // using email as login
        email: formValue.email,
        password: randomPassword,
        langKey: 'fr', // or dynamically set as needed
        type: 'client',
        firstName: formValue.firstName,
        lastName: formValue.lastName,
        phone: formValue.phone,
      };
      this.http.post('/api/register', payload).subscribe({
        next: () => {
          this.success = true;
          this.error = false;
          this.form.reset();
          this.submitted = false;
        },
        error: () => {
          this.error = true;
          this.success = false;
        },
      });
    }
  }

  loginWithProvider(provider: 'google'): void {
    const origin = window.location.origin;
    const redirectPath = '/login';
    const apiUrl = window.location.hostname === 'localhost' ? 'http://localhost:8080' : window.location.origin;
    window.location.href = `${apiUrl}/oauth2/authorization/${provider}?redirect_uri=${encodeURIComponent(origin)}&redirect_path=${encodeURIComponent(redirectPath)}`;
  }
}
