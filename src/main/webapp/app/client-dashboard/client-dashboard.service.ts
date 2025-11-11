import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

// ==========================================================================
// Interfaces
// ==========================================================================

export interface Ticket {
  id?: number;
  type: string;
  description: string;
  backofficeUrl?: string;
  backofficeLogin?: string;
  backofficePassword?: string;
  hostingUrl?: string;
  createdDate?: string;
  status?: string;
  imageUrl?: string;
  messages?: string[];
  messageStrings?: string[];
  priority?: 'low' | 'normal' | 'urgent';
}

export interface Project {
  id: number;
  name: string;
  description: string;
  progress: number;
  status: 'active' | 'testing' | 'completed';
  startDate: string;
  endDate?: string;
}

export interface Activity {
  id: number;
  activityType: 'SUCCESS' | 'INFO' | 'WARNING' | 'ERROR';
  title: string;
  description: string;
  timestamp: string;
  icon: string;
  userId?: number;
  ticketId?: number;
  entityType?: string;
  entityId?: number;
}

// ==========================================================================
// Service
// ==========================================================================

@Injectable({
  providedIn: 'root',
})
export class ClientDashboardService {
  private activitiesUrl = '/api/activities';
  private ticketsUrl = '/api/tickets';
  private projectsUrl = '/api/projects';

  constructor(private http: HttpClient) {}

  getActivities(): Observable<Activity[]> {
    return this.http.get<Activity[]>(this.activitiesUrl + '/recent/all');
  }

  getTickets(): Observable<Ticket[]> {
    return this.http.get<Ticket[]>(this.ticketsUrl);
  }

  getProjects(): Observable<Project[]> {
    return this.http.get<Project[]>(this.projectsUrl);
  }
}
