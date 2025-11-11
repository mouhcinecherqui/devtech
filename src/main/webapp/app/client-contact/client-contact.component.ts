import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { AlertService } from 'app/core/util/alert.service';

@Component({
  selector: 'jhi-client-contact',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule, TranslateModule],
  templateUrl: './client-contact.component.html',
  styleUrls: ['./client-contact.component.scss'],
})
export class ClientContactComponent implements OnInit {
  contactForm: FormGroup;
  isSubmitting = false;

  // Informations de contact de l'entreprise
  companyInfo = {
    name: 'DevTech',
    phone: '+33 1 23 45 67 89',
    email: 'contact@devtech.com',
    workingHours: 'Lundi - Vendredi: 9h00 - 18h00',
  };

  constructor(
    private fb: FormBuilder,
    private http: HttpClient,
    private alertService: AlertService,
    private translateService: TranslateService,
  ) {
    this.contactForm = this.fb.group({
      subject: ['', [Validators.required, Validators.minLength(5)]],
      message: ['', [Validators.required, Validators.minLength(20)]],
      priority: ['normal', Validators.required],
    });
  }

  ngOnInit(): void {
    // Component initialization logic if needed
  }

  onSubmit(): void {
    if (this.contactForm.valid && !this.isSubmitting) {
      this.isSubmitting = true;

      const formData = {
        subject: this.contactForm.value.subject,
        message: this.contactForm.value.message,
        priority: this.contactForm.value.priority,
        timestamp: new Date().toISOString(),
      };

      // Envoyer l'email à l'admin
      this.http.post('/api/contact/send-email', formData).subscribe({
        next: response => {
          this.alertService.addAlert({
            type: 'success',
            message: this.translateService.instant('contact.messages.success'),
          });
          this.contactForm.reset();
          this.isSubmitting = false;
        },
        error: error => {
          console.error("Erreur lors de l'envoi:", error);
          this.alertService.addAlert({
            type: 'danger',
            message: this.translateService.instant('contact.messages.error'),
          });
          this.isSubmitting = false;
        },
      });
    } else {
      this.markFormGroupTouched();
    }
  }

  getFieldError(fieldName: string): string {
    const field = this.contactForm.get(fieldName);
    if (field?.errors && field.touched) {
      if (field.errors['required']) {
        return this.translateService.instant('entity.validation.required');
      }
      if (field.errors['minlength']) {
        return this.translateService.instant('entity.validation.minlength', {
          min: field.errors['minlength'].requiredLength,
        });
      }
    }
    return '';
  }

  private markFormGroupTouched(): void {
    Object.keys(this.contactForm.controls).forEach(key => {
      const control = this.contactForm.get(key);
      control?.markAsTouched();
    });
  }
}
