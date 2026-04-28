import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { environment } from '../environments/environment';
import { LoginRequest } from '../models/login-request';

export interface User {
  username: string;
  roles: string[];
}

@Injectable({
  providedIn: 'root',
})
export class AuthService {
  constructor(private http: HttpClient) {}

  login(username: string, password: string): Observable<LoginRequest> {
    const dto: LoginRequest = {
      username: username,
      password: password,
    };
    return this.http.post<LoginRequest>(`${environment.apiUrl}/auth/login`, dto).pipe(
      tap((user) => {
        localStorage.setItem('credentials', JSON.stringify({ username, password }));
      }),
    );
  }

  logout(): void {
    localStorage.removeItem('currentUser');
    localStorage.removeItem('credentials');
    window.location.href = '/login';
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

  getAuthHeaders(): HttpHeaders {
    const credentialsStr = localStorage.getItem('credentials');
    if (!credentialsStr) {
      return new HttpHeaders();
    }

    try {
      const { username, password } = JSON.parse(credentialsStr);
      return new HttpHeaders({
        Authorization: 'Basic ' + btoa(`${username}:${password}`),
      });
    } catch {
      return new HttpHeaders();
    }
  }

  me(): Observable<User>{
    return this.http.get<User>(`${environment.apiUrl}/auth/me`).pipe(
      tap(user => {
        if (!localStorage.getItem('currentUser')){
          localStorage.setItem('currentUser',JSON.stringify(user));
        }
      })
    );
  }
}
