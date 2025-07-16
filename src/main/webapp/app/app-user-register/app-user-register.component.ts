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
      password: ['', [Validators.required, Validators.minLength(4)]],
      type: ['client'],
    });
  }

  submit(): void {
    if (this.form.valid) {
      this.submitted = true;
      this.http.post('/api/app-users', this.form.value).subscribe({
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
