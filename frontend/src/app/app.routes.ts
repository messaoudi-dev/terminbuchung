import { Routes } from '@angular/router';
import { Aerzte } from './arzt/aerzte/aerzte';
import { authGuard } from './auth/auth.guard';
import { Login } from './auth/login/login';
import { Registrierung } from './auth/registrierung/registrierung';
import { roleGuard } from './auth/role.guard';
import { Start } from './start/start';
import { TerminBuchen } from './termin/termin-buchen/termin-buchen';
import { Termine } from './termin/termine/termine';

export const routes: Routes = [
  { path: 'login', component: Login },
  { path: 'registrierung', component: Registrierung },
  { path: 'start', component: Start, canActivate: [authGuard] },
  { path: 'aerzte', component: Aerzte, canActivate: [authGuard] },
  { path: 'termine', component: Termine, canActivate: [authGuard] },
  { path: 'termine/buchen', component: TerminBuchen, canActivate: [authGuard, roleGuard('PATIENT')] },
  { path: '', pathMatch: 'full', redirectTo: 'start' },
  { path: '**', redirectTo: 'start' },
];
