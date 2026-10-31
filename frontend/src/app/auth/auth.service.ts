import { HttpClient } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { Observable, map, tap } from 'rxjs';
import { BenutzerResponse, LoginRequest, LoginResponse, RegisterRequest, Rolle } from './auth.models';

const TOKEN_KEY = 'token';

interface TokenClaims {
  sub: string;
  rolle: Rolle;
  email?: string;
  exp: number;
}

// Die Claims dienen nur der Anzeige und Navigation. Die Signatur prüft das Backend bei jedem Request.
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly gespeichertesToken = signal<string | null>(sessionStorage.getItem(TOKEN_KEY));

  login(request: LoginRequest): Observable<void> {
    return this.http.post<LoginResponse>('/api/auth/login', request).pipe(
      tap((response) => this.tokenSpeichern(response.token)),
      map(() => undefined),
    );
  }

  registrieren(request: RegisterRequest): Observable<BenutzerResponse> {
    return this.http.post<BenutzerResponse>('/api/auth/register', request);
  }

  abmelden(): void {
    sessionStorage.removeItem(TOKEN_KEY);
    this.gespeichertesToken.set(null);
  }

  token(): string | null {
    const token = this.gespeichertesToken();
    if (token === null || this.gueltigeClaims(token) === null) {
      return null;
    }
    return token;
  }

  istAngemeldet(): boolean {
    return this.token() !== null;
  }

  rolle(): Rolle | null {
    return this.aktuelleClaims()?.rolle ?? null;
  }

  // Ältere Tokens von vor dieser Änderung enthalten noch keine E-Mail.
  email(): string | null {
    return this.aktuelleClaims()?.email ?? null;
  }

  benutzerId(): number | null {
    const claims = this.aktuelleClaims();
    return claims === null ? null : Number(claims.sub);
  }

  private tokenSpeichern(token: string): void {
    sessionStorage.setItem(TOKEN_KEY, token);
    this.gespeichertesToken.set(token);
  }

  private aktuelleClaims(): TokenClaims | null {
    const token = this.gespeichertesToken();
    return token === null ? null : this.gueltigeClaims(token);
  }

  private gueltigeClaims(token: string): TokenClaims | null {
    const claims = leseClaims(token);
    if (claims === null || claims.exp * 1000 <= Date.now()) {
      return null;
    }
    return claims;
  }
}

// Ein JWT besteht aus Header.Payload.Signatur; der Payload ist Base64URL-kodiertes JSON.
function leseClaims(token: string): TokenClaims | null {
  try {
    const payload = token.split('.')[1];
    const base64 = payload.replace(/-/g, '+').replace(/_/g, '/');
    return JSON.parse(atob(base64)) as TokenClaims;
  } catch {
    return null;
  }
}
