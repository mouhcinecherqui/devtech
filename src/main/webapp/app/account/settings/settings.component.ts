import { Component, OnInit, inject, signal } from '@angular/core';
import { FormControl, FormGroup, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { TranslateService } from '@ngx-translate/core';

import SharedModule from 'app/shared/shared.module';
import { AccountService } from 'app/core/auth/account.service';
import { Account } from 'app/core/auth/account.model';
import { LANGUAGES } from 'app/config/language.constants';
import { PasswordService } from '../password/password.service';

const initialAccount: Account = {} as Account;

@Component({
  selector: 'jhi-settings',
  imports: [SharedModule, FormsModule, ReactiveFormsModule],
  templateUrl: './settings.component.html',
  styleUrl: './settings.component.scss',
})
export default class SettingsComponent implements OnInit {
  success = signal(false);
  passwordSuccess = signal(false);
  passwordError = signal(false);
  showPasswordForm = signal(false);
  isOAuth2User = signal(false);
  languages = LANGUAGES;

  settingsForm = new FormGroup({
    id: new FormControl(null),
    firstName: new FormControl(initialAccount.firstName, {
      nonNullable: true,
      validators: [Validators.required, Validators.minLength(1), Validators.maxLength(50)],
    }),
    lastName: new FormControl(initialAccount.lastName, {
      nonNullable: true,
      validators: [Validators.required, Validators.minLength(1), Validators.maxLength(50)],
    }),
    email: new FormControl(initialAccount.email, {
      nonNullable: true,
      validators: [Validators.required, Validators.minLength(5), Validators.maxLength(254), Validators.email],
    }),
    phone: new FormControl('', { nonNullable: true, validators: [Validators.minLength(6), Validators.maxLength(20)] }),
    langKey: new FormControl(initialAccount.langKey, { nonNullable: true }),

    activated: new FormControl(initialAccount.activated, { nonNullable: true }),
    authorities: new FormControl(initialAccount.authorities, { nonNullable: true }),
    imageUrl: new FormControl(initialAccount.imageUrl, { nonNullable: true }),
    login: new FormControl(initialAccount.login, { nonNullable: true }),
  });

  passwordForm = new FormGroup({
    currentPassword: new FormControl('', { nonNullable: true }),
    newPassword: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, Validators.minLength(4), Validators.maxLength(100)],
    }),
    confirmPassword: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
  });

  private readonly accountService = inject(AccountService);
  private readonly translateService = inject(TranslateService);
  private readonly passwordService = inject(PasswordService);

  ngOnInit(): void {
    this.accountService.identity().subscribe(account => {
      if (account) {
        this.settingsForm.patchValue(account as any);
        // Check if user is OAuth2 (has ROLE_CLIENT or authorities contain CLIENT)
        this.isOAuth2User.set(account.authorities.includes('ROLE_CLIENT'));

        // For OAuth2 users, remove currentPassword requirement
        if (this.isOAuth2User()) {
          this.passwordForm.get('currentPassword')?.clearValidators();
        } else {
          this.passwordForm.get('currentPassword')?.setValidators([Validators.required]);
        }
        this.passwordForm.get('currentPassword')?.updateValueAndValidity();
      }
    });
  }

  save(): void {
    this.success.set(false);

    const formData = this.settingsForm.getRawValue();

    // Create AdminUserDTO compatible object
    const adminUserDTO = {
      id: formData.id,
      login: formData.login,
      firstName: formData.firstName,
      lastName: formData.lastName,
      email: formData.email,
      imageUrl: formData.imageUrl,
      activated: formData.activated,
      langKey: formData.langKey,
      authorities: formData.authorities,
      phone: formData.phone, // Add phone field for AppUser
    };

    this.accountService.save(adminUserDTO as any).subscribe({
      next: () => {
        this.success.set(true);
        this.accountService.authenticate(adminUserDTO as any);

        if (adminUserDTO.langKey !== this.translateService.currentLang) {
          this.translateService.use(adminUserDTO.langKey);
        }
      },
      error(error) {
        console.error('Error saving account:', error);
        // You could add error handling here if needed
      },
    });
  }

  togglePasswordForm(): void {
    this.showPasswordForm.set(!this.showPasswordForm());
    if (!this.showPasswordForm()) {
      this.passwordForm.reset();
      this.passwordSuccess.set(false);
      this.passwordError.set(false);
    }
  }

  changePassword(): void {
    this.passwordError.set(false);
    this.passwordSuccess.set(false);

    const newPassword = this.passwordForm.get('newPassword')?.value;
    const confirmPassword = this.passwordForm.get('confirmPassword')?.value;
    const currentPassword = this.passwordForm.get('currentPassword')?.value;

    if (newPassword !== confirmPassword) {
      this.passwordError.set(true);
      return;
    }

    if (this.isOAuth2User()) {
      // For OAuth2 users, use the set-password endpoint
      if (this.passwordForm.get('newPassword')?.valid && this.passwordForm.get('confirmPassword')?.valid) {
        this.passwordService.setPassword(newPassword!).subscribe({
          next: () => {
            this.passwordSuccess.set(true);
            this.passwordForm.reset();
            setTimeout(() => {
              this.showPasswordForm.set(false);
              this.passwordSuccess.set(false);
            }, 3000);
          },
          error: () => {
            this.passwordError.set(true);
          },
        });
      }
    } else {
      // For traditional users, use the change-password endpoint
      if (this.passwordForm.valid) {
        this.passwordService.save(newPassword!, currentPassword!).subscribe({
          next: () => {
            this.passwordSuccess.set(true);
            this.passwordForm.reset();
            setTimeout(() => {
              this.showPasswordForm.set(false);
              this.passwordSuccess.set(false);
            }, 3000);
          },
          error: () => {
            this.passwordError.set(true);
          },
        });
      }
    }
  }
}
