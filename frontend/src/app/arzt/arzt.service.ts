import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { ArztRequest, ArztResponse } from './arzt.models';

@Injectable({ providedIn: 'root' })
export class ArztService {
  private readonly http = inject(HttpClient);

  findeAlle(): Observable<ArztResponse[]> {
    return this.http.get<ArztResponse[]>('/api/aerzte');
  }

  anlegen(request: ArztRequest): Observable<ArztResponse> {
    return this.http.post<ArztResponse>('/api/aerzte', request);
  }

  aendern(id: number, request: ArztRequest): Observable<ArztResponse> {
    return this.http.put<ArztResponse>(`/api/aerzte/${id}`, request);
  }

  loeschen(id: number): Observable<void> {
    return this.http.delete<void>(`/api/aerzte/${id}`);
  }
}
