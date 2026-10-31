import { Routes } from '@angular/router';
import { authGuard } from './auth/auth.guard';
import { Login } from './auth/login/login';
import { Registrierung } from './auth/registrierung/registrierung';
import { roleGuard } from './auth/role.guard';
import { Platzhalter } from './platzhalter/platzhalter';
import { Start } from './start/start';

export const routes: Routes = [
  { path: 'login', component: Login },
  { path: 'registrierung', component: Registrierung },
  { path: 'start', component: Start, canActivate: [authGuard] },
  { path: 'aerzte', component: Platzhalter, canActivate: [authGuard], data: { titel: 'Ärzte' } },
  { path: 'termine', component: Platzhalter, canActivate: [authGuard], data: { titel: 'Termine' } },
  {
    path: 'termine/buchen',
    component: Platzhalter,
    canActivate: [authGuard, roleGuard('PATIENT')],
    data: { titel: 'Termin buchen' },
  },
  { path: '', pathMatch: 'full', redirectTo: 'start' },
  { path: '**', redirectTo: 'start' },
];
