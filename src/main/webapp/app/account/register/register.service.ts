import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { ApplicationConfigService } from 'app/core/config/application-config.service';
import { Registration } from './register.model';

@Injectable({ providedIn: 'root' })
export class RegisterService {
  private readonly http = inject(HttpClient);
  private readonly applicationConfigService = inject(ApplicationConfigService);

  save(registration: Registration): Observable<{}> {
    return this.http.post(this.applicationConfigService.getEndpointFor('api/register'), { ...registration, type: 'client' });
  }
}
