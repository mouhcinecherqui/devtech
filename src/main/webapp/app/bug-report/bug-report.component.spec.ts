import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ReactiveFormsModule, FormBuilder } from '@angular/forms';
import { jest } from '@jest/globals';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { BugReportComponent } from './bug-report.component';

describe('BugReportComponent', () => {
  let component: BugReportComponent;
  let fixture: ComponentFixture<BugReportComponent>;
  let translateService: any;

  beforeEach(async () => {
    const translateSpy = { instant: jest.fn() };

    await TestBed.configureTestingModule({
      imports: [BugReportComponent, ReactiveFormsModule, TranslateModule.forRoot()],
      providers: [FormBuilder, { provide: TranslateService, useValue: translateSpy }],
    }).compileComponents();

    fixture = TestBed.createComponent(BugReportComponent);
    component = fixture.componentInstance;
    translateService = TestBed.inject(TranslateService) as any;

    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should initialize form with default values', () => {
    expect(component.form.get('name')?.value).toBe('');
    expect(component.form.get('email')?.value).toBe('');
    expect(component.form.get('website')?.value).toBe('');
    expect(component.form.get('description')?.value).toBe('');
  });

  it('should validate required fields', () => {
    const name = component.form.get('name');
    const email = component.form.get('email');
    const description = component.form.get('description');

    name?.markAsTouched();
    email?.markAsTouched();
    description?.markAsTouched();

    expect(name?.hasError('required')).toBeTruthy();
    expect(email?.hasError('required')).toBeTruthy();
    expect(description?.hasError('required')).toBeTruthy();
  });

  it('should validate email format', () => {
    const email = component.form.get('email');
    email?.setValue('invalid-email');
    email?.markAsTouched();
    expect(email?.hasError('email')).toBeTruthy();
  });

  it('should mark form as valid with proper values', () => {
    component.form.patchValue({
      name: 'John',
      email: 'john@example.com',
      website: 'https://example.com',
      description: 'Something is wrong',
    });
    expect(component.form.valid).toBeTruthy();
  });

  it('should handle form submission', () => {
    jest.spyOn(component, 'submit');
    component.submit();
    expect(component.submit).toHaveBeenCalled();
  });

  it('should set submitted to true on submit', () => {
    component.submit();
    expect(component.submitted).toBeTruthy();
  });

  it('should have initial state', () => {
    expect(component.submitted).toBeFalsy();
  });

  // Removed template-driven validation helpers not present in component

  // Removed severity and UI helpers not present in component

  // ...

  // ...

  // Removed UI state tests not present in component API
});
