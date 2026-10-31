export type TerminStatus = 'GEBUCHT' | 'STORNIERT';

export interface TerminRequest {
  arztId: number;
  beginn: string;
}

export interface TerminResponse {
  id: number;
  arztId: number;
  arztName: string;
  patientId: number;
  beginn: string;
  ende: string;
  status: TerminStatus;
}
