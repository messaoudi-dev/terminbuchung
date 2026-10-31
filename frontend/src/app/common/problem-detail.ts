import { HttpErrorResponse } from '@angular/common/http';

export interface ProblemDetail {
  title?: string;
  status?: number;
  detail?: string;
  errors?: Record<string, string>;
}

export interface Fehlermeldung {
  text: string;
  feldFehler: string[];
}

export function leseFehler(error: unknown): Fehlermeldung {
  const problem = error instanceof HttpErrorResponse ? (error.error as ProblemDetail | null) : null;
  if (problem !== null && typeof problem === 'object' && typeof problem.detail === 'string') {
    return { text: problem.detail, feldFehler: Object.values(problem.errors ?? {}) };
  }
  // Kein ProblemDetail, z. B. wenn das Backend nicht läuft und der Proxy antwortet.
  return {
    text: 'Die Anfrage ist fehlgeschlagen. Bitte versuche es später erneut.',
    feldFehler: [],
  };
}
