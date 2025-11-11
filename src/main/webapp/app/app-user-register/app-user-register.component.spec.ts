import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ReactiveFormsModule, FormBuilder } from '@angular/forms';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { jest } from '@jest/globals';
import { AppUserRegisterComponent } from './app-user-register.component';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';

describe('AppUserRegisterComponent', () => {
  let component: AppUserRegisterComponent;
  let fixture: ComponentFixture<AppUserRegisterComponent>;
  let translateService: any;

  let httpMock: HttpTestingController;

  beforeEach(async () => {
    const translateSpy = {
      instant: jest.fn((key: string) => key),
      get: jest.fn(() => ({
        subscribe(arg: any) {
          if (typeof arg === 'function') {
            arg('');
          } else if (arg && typeof arg.next === 'function') {
            arg.next('');
          }
          return { unsubscribe() {} };
        },
      })),
      onTranslationChange: { subscribe: jest.fn(() => ({ unsubscribe() {} })) },
      onLangChange: { subscribe: jest.fn(() => ({ unsubscribe() {} })) },
      onDefaultLangChange: { subscribe: jest.fn(() => ({ unsubscribe() {} })) },
    } as any;

    await TestBed.configureTestingModule({
      imports: [AppUserRegisterComponent, ReactiveFormsModule, TranslateModule.forRoot()],
      providers: [FormBuilder, provideHttpClient(), provideHttpClientTesting(), { provide: TranslateService, useValue: translateSpy }],
    }).compileComponents();

    fixture = TestBed.createComponent(AppUserRegisterComponent);
    component = fixture.componentInstance;
    translateService = TestBed.inject(TranslateService) as any;
    httpMock = TestBed.inject(HttpTestingController);

    fixture.detectChanges();
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should initialize form with default values', () => {
    expect(component.form.get('firstName')?.value).toBe('');
    expect(component.form.get('lastName')?.value).toBe('');
    expect(component.form.get('email')?.value).toBe('');
    expect(component.form.get('phone')?.value).toBe('');
    expect(component.form.get('type')?.value).toBe('client');
  });

  it('should validate required fields', () => {
    const firstName = component.form.get('firstName');
    const lastName = component.form.get('lastName');
    const email = component.form.get('email');
    const phone = component.form.get('phone');

    firstName?.markAsTouched();
    lastName?.markAsTouched();
    email?.markAsTouched();
    phone?.markAsTouched();

    expect(firstName?.hasError('required')).toBeTruthy();
    expect(lastName?.hasError('required')).toBeTruthy();
    expect(email?.hasError('required')).toBeTruthy();
    expect(phone?.hasError('required')).toBeTruthy();
  });

  it('should validate email format', () => {
    const email = component.form.get('email');
    email?.setValue('invalid-email');
    email?.markAsTouched();
    expect(email?.hasError('email')).toBeTruthy();
  });

  it('should not error when form becomes valid', () => {
    component.form.patchValue({ firstName: 'a', lastName: 'b', email: 'a@a.com', phone: '1' });
    expect(component.form.valid).toBeTruthy();
  });

  it('should POST /api/register on submit success and reset flags/form', () => {
    component.form.patchValue({ firstName: 'a', lastName: 'b', email: 'a@a.com', phone: '1', type: 'client' });
    (component as any).submit();

    const req = httpMock.expectOne('/api/register');
    expect(req.request.method).toBe('POST');
    expect(req.request.body.email).toBe('a@a.com');
    req.flush({});

    expect(component.success).toBeTruthy();
    expect(component.error).toBeFalsy();
  });

  it('should set error on submit failure', () => {
    component.form.patchValue({ firstName: 'a', lastName: 'b', email: 'a@a.com', phone: '1', type: 'client' });
    (component as any).submit();

    const req = httpMock.expectOne('/api/register');
    req.error(new ProgressEvent('error'));

    expect(component.error).toBeTruthy();
    expect(component.success).toBeFalsy();
  });

  it('should mark form as valid with proper values', () => {
    component.form.patchValue({
      firstName: 'John',
      lastName: 'Doe',
      email: 'john.doe@example.com',
      phone: '0600000000',
      type: 'client',
    });
    expect(component.form.valid).toBeTruthy();
  });

  it('should expose submit method', () => {
    expect(typeof (component as any).submit).toBe('function');
  });

  it('should have initial flags', () => {
    expect(component.submitted).toBeFalsy();
    expect(component.success).toBeFalsy();
    expect(component.error).toBeFalsy();
  });
});
