export type Rolle = 'PRAXIS' | 'PATIENT';

export interface LoginRequest {
  email: string;
  passwort: string;
}

export interface LoginResponse {
  token: string;
  expiresIn: number;
}

export interface RegisterRequest {
  email: string;
  passwort: string;
}

export interface BenutzerResponse {
  id: number;
  email: string;
  rolle: Rolle;
}
