import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { Rolle } from './auth.models';
import { AuthService } from './auth.service';

// Nur für die Navigation. Die echten Rechte prüft das Backend.
export function roleGuard(rolle: Rolle): CanActivateFn {
  return () => {
    const authService = inject(AuthService);
    const router = inject(Router);

    if (authService.rolle() === rolle) {
      return true;
    }
    return router.createUrlTree(['/start']);
  };
}
