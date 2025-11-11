import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface OAuth2UserInfo {
  success: boolean;
  email?: string;
  name?: string;
  picture?: string;
  appUserId?: number;
  oauth2?: boolean;
  appUser?: {
    id: number;
    firstName: string;
    lastName: string;
    email: string;
    phone: string;
    type: string;
    createdDate: string;
  };
  message?: string;
}

@Injectable({
  providedIn: 'root',
})
export class OAuth2UserService {
  constructor(private http: HttpClient) {}

  /**
   * Récupère les informations de l'utilisateur connecté via OAuth2
   */
  getOAuth2UserInfo(): Observable<OAuth2UserInfo> {
    return this.http.get<OAuth2UserInfo>('/api/oauth2/user');
  }

  /**
   * Met à jour les informations de l'utilisateur OAuth2
   */
  updateOAuth2User(): Observable<OAuth2UserInfo> {
    return this.http.get<OAuth2UserInfo>('/api/oauth2/user/update');
  }

  /**
   * Vérifie si l'utilisateur actuel est connecté via OAuth2
   */
  isOAuth2User(): Observable<boolean> {
    return new Observable(observer => {
      this.getOAuth2UserInfo().subscribe({
        next(response) {
          observer.next(response.success && response.oauth2 === true);
          observer.complete();
        },
        error() {
          observer.next(false);
          observer.complete();
        },
      });
    });
  }
}
