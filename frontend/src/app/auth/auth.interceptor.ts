import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { AuthService } from './auth.service';

export const authInterceptor: HttpInterceptorFn = (request, next) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  // Bei /api/auth bedeutet 401 "falsches Passwort". Die Login-Seite soll diese Meldung selbst anzeigen.
  const istAuthRequest = request.url.startsWith('/api/auth/');
  const istApiRequest = request.url.startsWith('/api/');
  const token = authService.token();

  const anfrage =
    istApiRequest && !istAuthRequest && token !== null
      ? request.clone({ setHeaders: { Authorization: `Bearer ${token}` } })
      : request;

  return next(anfrage).pipe(
    catchError((error: unknown) => {
      if (error instanceof HttpErrorResponse && error.status === 401 && !istAuthRequest) {
        authService.abmelden();
        router.navigate(['/login']);
      }
      return throwError(() => error);
    }),
  );
};
