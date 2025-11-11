import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { map, shareReplay, catchError } from 'rxjs/operators';
import { Observable, of } from 'rxjs';

import { ApplicationConfigService } from 'app/core/config/application-config.service';
import { InfoResponse, ProfileInfo } from './profile-info.model';

@Injectable({ providedIn: 'root' })
export class ProfileService {
  private readonly http = inject(HttpClient);
  private readonly applicationConfigService = inject(ApplicationConfigService);

  private readonly infoUrl = this.applicationConfigService.getEndpointFor('management/info');
  private profileInfo$?: Observable<ProfileInfo>;

  getProfileInfo(): Observable<ProfileInfo> {
    if (this.profileInfo$) {
      return this.profileInfo$;
    }

    this.profileInfo$ = this.http.get<InfoResponse>(this.infoUrl).pipe(
      map((response: InfoResponse) => {
        const profileInfo: ProfileInfo = {
          activeProfiles: response.activeProfiles,
          inProduction: response.activeProfiles?.includes('prod'),
          openAPIEnabled: response.activeProfiles?.includes('api-docs'),
        };
        if (response.activeProfiles && response['display-ribbon-on-profiles']) {
          const displayRibbonOnProfiles = response['display-ribbon-on-profiles'].split(',');
          const ribbonProfiles = displayRibbonOnProfiles.filter(profile => response.activeProfiles?.includes(profile));
          if (ribbonProfiles.length > 0) {
            profileInfo.ribbonEnv = ribbonProfiles[0];
          }
        }
        return profileInfo;
      }),
      catchError(error => {
        console.warn('Profile info not available, using defaults:', error);
        // Retourner des valeurs par défaut en cas d'erreur
        const defaultProfileInfo: ProfileInfo = {
          activeProfiles: ['dev'],
          inProduction: false,
          openAPIEnabled: true,
        };
        return of(defaultProfileInfo);
      }),
      shareReplay(),
    );
    return this.profileInfo$;
  }
}
