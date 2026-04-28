import { Injectable } from '@angular/core';
import { CanActivate, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

@Injectable({
  providedIn: 'root',
})
export class AuthGuard implements CanActivate {
  constructor(
    private auth: AuthService,
    private router: Router,
  ) {}

  canActivate(): boolean {
    const isLoggedIn = this.auth.isLoggedIn();
    const path = window.location.pathname;
    if (isLoggedIn) {
      return true;
    }
    console.log('Redirecting to /login');
    this.router.navigate(['/login']);
    return false;
  }
}
