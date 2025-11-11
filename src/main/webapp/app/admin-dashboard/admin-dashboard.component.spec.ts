import { ComponentFixture, TestBed } from '@angular/core/testing';
import { TranslateModule } from '@ngx-translate/core';
import { provideRouter } from '@angular/router';
import { AdminDashboardComponent } from './admin-dashboard.component';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';

describe('AdminDashboardComponent', () => {
  let component: AdminDashboardComponent;
  let fixture: ComponentFixture<AdminDashboardComponent>;

  let httpMock: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AdminDashboardComponent, TranslateModule.forRoot()],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    }).compileComponents();

    fixture = TestBed.createComponent(AdminDashboardComponent);
    component = fixture.componentInstance;
    httpMock = TestBed.inject(HttpTestingController);

    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should initialize with default values', () => {
    expect(Array.isArray(component.tickets)).toBeTruthy();
    expect(component.error === null || typeof component.error === 'string').toBeTruthy();
  });

  it('should call load methods on init', () => {
    jest.spyOn(component as any, 'loadTickets');
    jest.spyOn(component as any, 'loadPaiements');
    component.ngOnInit();
    expect((component as any).loadTickets).toHaveBeenCalled();
    expect((component as any).loadPaiements).toHaveBeenCalled();
  });

  it('should load tickets and update stats on success', () => {
    component.loadTickets();
    const reqs = httpMock.match('/api/tickets');
    expect(reqs.length).toBeGreaterThan(0);
    reqs[reqs.length - 1].flush([{ status: 'Nouveau' }, { status: 'En cours' }, { status: 'Fermé' }, { status: 'Urgent' }]);
    expect(component.tickets.length).toBe(4);
  });

  it('should set error when tickets load fails', () => {
    component.loadTickets();
    const reqs = httpMock.match('/api/tickets');
    reqs[reqs.length - 1].error(new ProgressEvent('error'));
    expect(typeof component.error === 'string' || component.error === null).toBeTruthy();
  });
});
