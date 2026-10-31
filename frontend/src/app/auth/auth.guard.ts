import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from './auth.service';

export const authGuard: CanActivateFn = () => {
  const authService = inject(AuthService);
  const router = inject(Router);

  if (authService.istAngemeldet()) {
    return true;
  }
  // Entfernt auch ein abgelaufenes Token aus dem sessionStorage.
  authService.abmelden();
  return router.createUrlTree(['/login']);
};
