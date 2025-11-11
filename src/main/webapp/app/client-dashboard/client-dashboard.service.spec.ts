import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { ClientDashboardService } from './client-dashboard.service';

describe('ClientDashboardService', () => {
  let service: ClientDashboardService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(ClientDashboardService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should get activities', () => {
    const mockActivities: any[] = [];
    let received: any;
    service.getActivities().subscribe(a => (received = a));
    const req = httpMock.expectOne('/api/activities/recent/all');
    expect(req.request.method).toBe('GET');
    req.flush(mockActivities);
    expect(received).toEqual(mockActivities);
  });

  it('should get tickets', () => {
    const mockTickets: any[] = [];
    let received: any;
    service.getTickets().subscribe(t => (received = t));
    const req = httpMock.expectOne('/api/tickets');
    expect(req.request.method).toBe('GET');
    req.flush(mockTickets);
    expect(received).toEqual(mockTickets);
  });

  it('should get projects', () => {
    const mockProjects: any[] = [];
    let received: any;
    service.getProjects().subscribe(p => (received = p));
    const req = httpMock.expectOne('/api/projects');
    expect(req.request.method).toBe('GET');
    req.flush(mockProjects);
    expect(received).toEqual(mockProjects);
  });
});
