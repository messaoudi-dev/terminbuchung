import { Component, inject } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';

export interface BestaetigungsDaten {
  titel: string;
  text: string;
  bestaetigen: string;
}

@Component({
  selector: 'app-bestaetigungs-dialog',
  imports: [MatButtonModule, MatDialogModule],
  template: `
    <h2 mat-dialog-title>{{ daten.titel }}</h2>
    <mat-dialog-content>{{ daten.text }}</mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button type="button" [mat-dialog-close]="false">Abbrechen</button>
      <button mat-flat-button type="button" [mat-dialog-close]="true">{{ daten.bestaetigen }}</button>
    </mat-dialog-actions>
  `,
})
export class BestaetigungsDialog {
  protected readonly daten: BestaetigungsDaten = inject(MAT_DIALOG_DATA);
}
