import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { catchError, Observable, of, tap } from 'rxjs';
import { environment } from '../environments/environment';
import { MonitoringSessionService } from './monitoring-session.service';

export interface User {
  username: string;
  roles: string[];
}

@Injectable({
  providedIn: 'root',
})
export class AuthService {
  constructor(
    private http: HttpClient,
    private monitoringSession: MonitoringSessionService,
  ) {}

  login(username: string, password: string): Observable<User> {
    const dto = {
      username: username,
      password: password,
    };
    return this.http.post<User>(`${environment.apiUrl}/auth/login`, dto).pipe(
      tap(() => {
        this.monitoringSession.reset();
        localStorage.setItem('credentials', JSON.stringify({ username, password }));
      }),
      tap((user) => {
        localStorage.setItem('currentUser', JSON.stringify(user));
      }),
    );
  }

  logout(): void {
    this.monitoringSession.reset();
    localStorage.removeItem('currentUser');
    localStorage.removeItem('credentials');
    this.http.post(`${environment.apiUrl}/auth/logout`, {}).pipe(catchError(() => of(null))).subscribe();
  }

  isLoggedIn(): boolean {
    return localStorage.getItem('currentUser') !== null;
  }

  getCurrentUser(): User | null {
    const userStr = localStorage.getItem('currentUser');
    if (!userStr) return null;
    try {
      return JSON.parse(userStr);
    } catch {
      return null;
    }
  }

  isAdmin(): boolean {
    const user = this.getCurrentUser();
    if (!user) return false;
    return (
      user.roles.includes('ROLE_ADMIN') ||
      user.roles.includes('ADMIN') ||
      user.roles.some((role) => role.endsWith('ADMIN'))
    );
  }

  /** HTTP Basic на каждый запрос — без серверной сессии (STATELESS). */
  getAuthHeaders(): HttpHeaders {
    const credentialsStr = localStorage.getItem('credentials');
    if (!credentialsStr) {
      return new HttpHeaders();
    }
    try {
      const { username, password } = JSON.parse(credentialsStr) as {
        username: string;
        password: string;
      };
      return new HttpHeaders({
        Authorization: 'Basic ' + btoa(`${username}:${password}`),
      });
    } catch {
      return new HttpHeaders();
    }
  }

  me(): Observable<User> {
    return this.http.get<User>(`${environment.apiUrl}/auth/me`).pipe(
      tap((user) => {
        localStorage.setItem('currentUser', JSON.stringify(user));
      }),
    );
  }
}
