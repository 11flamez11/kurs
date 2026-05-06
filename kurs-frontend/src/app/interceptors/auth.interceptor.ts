import { Injectable } from '@angular/core';
import {
  HttpInterceptor,
  HttpRequest,
  HttpHandler,
  HttpEvent,
  HttpErrorResponse,
} from '@angular/common/http';
import { catchError, Observable, throwError } from 'rxjs';
import { AuthService } from '../services/auth.service';

@Injectable()
export class AuthInterceptor implements HttpInterceptor {
  constructor(private auth: AuthService) {}

  intercept(req: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    if (req.url.includes('/api/auth/login') || req.url.includes('/api/users/register')) {
      return next.handle(req).pipe(
        catchError((error: HttpErrorResponse) => throwError(() => error)),
      );
    }

    const authHeaders = this.auth.getAuthHeaders();
    const authorization = authHeaders.get('Authorization');
    const authReq = authorization
      ? req.clone({
          setHeaders: { Authorization: authorization },
        })
      : req;

    return next.handle(authReq);
  }
}
