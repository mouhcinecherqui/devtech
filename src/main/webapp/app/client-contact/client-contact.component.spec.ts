import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ReactiveFormsModule, FormBuilder } from '@angular/forms';
import { jest } from '@jest/globals';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { AlertService } from 'app/core/util/alert.service';
import { ClientContactComponent } from './client-contact.component';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';

describe('ClientContactComponent', () => {
  let component: ClientContactComponent;
  let fixture: ComponentFixture<ClientContactComponent>;
  let alertService: any;
  let translateService: any;

  let httpMock: HttpTestingController;

  beforeEach(async () => {
    const alertSpy = { addAlert: jest.fn() } as Partial<AlertService>;
    const translateSpy = {
      instant: jest.fn((key: string) => key),
      get: jest.fn(() => ({
        subscribe(arg: any) {
          if (typeof arg === 'function') {
            arg('');
          } else if (arg && typeof arg.next === 'function') {
            arg.next('');
          }
          return { unsubscribe() {} } as any;
        },
      })),
      onTranslationChange: { subscribe: jest.fn(() => ({ unsubscribe() {} })) },
      onLangChange: { subscribe: jest.fn(() => ({ unsubscribe() {} })) },
      onDefaultLangChange: { subscribe: jest.fn(() => ({ unsubscribe() {} })) },
    } as any;

    await TestBed.configureTestingModule({
      imports: [ClientContactComponent, ReactiveFormsModule, TranslateModule.forRoot()],
      providers: [
        FormBuilder,
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: AlertService, useValue: alertSpy },
        { provide: TranslateService, useValue: translateSpy },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ClientContactComponent);
    component = fixture.componentInstance;
    alertService = TestBed.inject(AlertService) as any;
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
    expect(component.contactForm.get('subject')?.value).toBe('');
    expect(component.contactForm.get('message')?.value).toBe('');
    expect(component.contactForm.get('priority')?.value).toBe('normal');
  });

  it('should validate required fields', () => {
    const subjectControl = component.contactForm.get('subject');
    const messageControl = component.contactForm.get('message');

    subjectControl?.markAsTouched();
    messageControl?.markAsTouched();

    expect(subjectControl?.hasError('required')).toBeTruthy();
    expect(messageControl?.hasError('required')).toBeTruthy();
  });

  it('should validate minimum length for subject', () => {
    const subjectControl = component.contactForm.get('subject');
    subjectControl?.setValue('Hi');
    subjectControl?.markAsTouched();

    expect(subjectControl?.hasError('minlength')).toBeTruthy();
  });

  it('should validate minimum length for message', () => {
    const messageControl = component.contactForm.get('message');
    messageControl?.setValue('Short');
    messageControl?.markAsTouched();

    expect(messageControl?.hasError('minlength')).toBeTruthy();
  });

  it('should mark form as valid with proper values', () => {
    component.contactForm.patchValue({
      subject: 'Test Subject',
      message: 'This is a test message with sufficient length',
      priority: 'high',
    });

    expect(component.contactForm.valid).toBeTruthy();
  });

  it('should display company information correctly', () => {
    expect(component.companyInfo.name).toBe('DevTech');
    expect(component.companyInfo.phone).toBe('+33 1 23 45 67 89');
    expect(component.companyInfo.email).toBe('contact@devtech.com');
  });

  it('should not trigger HTTP when just validating form', () => {
    component.contactForm.patchValue({
      subject: 'Test Subject',
      message: 'This is a test message with sufficient length',
      priority: 'normal',
    });
    expect(component.contactForm.valid).toBeTruthy();
  });

  it('should not submit form when invalid', () => {
    jest.spyOn(component, 'onSubmit');
    component.contactForm.patchValue({
      subject: '',
      message: '',
      priority: 'normal',
    });

    component.onSubmit();
    expect(component.isSubmitting).toBeFalsy();
  });

  it('should get field error for required validation', () => {
    translateService.instant.mockReturnValue('This field is required');
    component.contactForm.get('subject')?.setValue('');
    component.contactForm.get('subject')?.markAsTouched();
    const error = component.getFieldError('subject');
    expect(translateService.instant).toHaveBeenCalledWith('entity.validation.required');
  });

  it('should get field error for minlength validation', () => {
    translateService.instant.mockReturnValue('Minimum length required');
    component.contactForm.get('subject')?.setValue('Hi');
    component.contactForm.get('subject')?.markAsTouched();

    const error = component.getFieldError('subject');
    expect(translateService.instant).toHaveBeenCalledWith('entity.validation.minlength', expect.any(Object));
  });

  it('should POST contact and handle success', () => {
    // arrange
    translateService.instant.mockReturnValue('ok');
    component.contactForm.patchValue({ subject: 'sujet', message: 'message long suffisant', priority: 'normal' });

    // act
    component.onSubmit();

    const req = httpMock.expectOne('/api/contact/send-email');
    expect(req.request.method).toBe('POST');
    expect(req.request.body.subject).toBe('sujet');
    expect(req.request.body.message).toBe('message long suffisant');
    req.flush({ message: 'ok' });

    // assert
    expect(alertService.addAlert).toHaveBeenCalled();
    expect(component.isSubmitting).toBeFalsy();
    expect(component.contactForm.get('subject')?.value).toBeNull();
  });

  it('should handle API error on submit', () => {
    translateService.instant.mockReturnValue('error');
    component.contactForm.patchValue({ subject: 'sujet', message: 'message long suffisant', priority: 'normal' });

    component.onSubmit();

    const req = httpMock.expectOne('/api/contact/send-email');
    req.error(new ProgressEvent('error'));

    expect(alertService.addAlert).toHaveBeenCalled();
    expect(component.isSubmitting).toBeFalsy();
  });
});
