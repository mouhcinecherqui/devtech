import { Component } from '@angular/core';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-user-register',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
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

  loginWithProvider(provider: 'google') {
    window.location.href = `/oauth2/authorization/${provider}`;
  }
}
