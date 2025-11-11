import { TestBed } from '@angular/core/testing';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { ClientTicketsService } from './client-tickets.service';

describe('ClientTicketsService', () => {
  let service: ClientTicketsService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [ClientTicketsService, provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(ClientTicketsService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should get my tickets (observable)', () => {
    const mockTickets = [
      { id: 1, title: 'Ticket 1', description: 'd1' },
      { id: 2, title: 'Ticket 2', description: 'd2' },
    ];

    let received: any;
    service.getMyTickets().subscribe(t => (received = t));

    const req = httpMock.expectOne('/api/tickets');
    expect(req.request.method).toBe('GET');
    req.flush(mockTickets);

    expect(received).toEqual(mockTickets);
  });

  it('should create a new ticket (observable)', () => {
    const newTicket = { title: 'New Ticket', description: 'Test description' } as any;
    const mockResponse = { id: 1, ...newTicket };

    let received: any;
    service.createTicket(newTicket).subscribe(ticket => (received = ticket));

    const req = httpMock.expectOne('/api/tickets');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(newTicket);
    req.flush(mockResponse);

    expect(received).toEqual(mockResponse);
  });

  // Service only exposes getMyTickets/createTicket; other tests removed

  it('should handle error when getting tickets', () => {
    let errorCaught = false;
    service.getMyTickets().subscribe({ error: () => (errorCaught = true) });

    const req = httpMock.expectOne('/api/tickets');
    req.error(new ProgressEvent('Network error'));

    expect(errorCaught).toBeTruthy();
  });

  it('should handle error when creating ticket', () => {
    const newTicket = { title: 'New Ticket', description: 'Test description' } as any;
    let errorCaught = false;
    service.createTicket(newTicket).subscribe({ error: () => (errorCaught = true) });

    const req = httpMock.expectOne('/api/tickets');
    req.error(new ProgressEvent('Validation error'));

    expect(errorCaught).toBeTruthy();
  });
});
