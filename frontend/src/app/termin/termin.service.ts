import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { TerminRequest, TerminResponse } from './termin.models';

@Injectable({ providedIn: 'root' })
export class TerminService {
  private readonly http = inject(HttpClient);

  findeAlle(): Observable<TerminResponse[]> {
    return this.http.get<TerminResponse[]>('/api/termine');
  }

  buchen(request: TerminRequest): Observable<TerminResponse> {
    return this.http.post<TerminResponse>('/api/termine', request);
  }

  stornieren(id: number): Observable<TerminResponse> {
    return this.http.post<TerminResponse>(`/api/termine/${id}/stornieren`, null);
  }
}
