import { Component, inject, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { Observable } from 'rxjs';
import { Fehlermeldung, leseFehler } from '../../common/problem-detail';
import { ArztResponse } from '../arzt.models';
import { ArztService } from '../arzt.service';

// Ohne Arzt legt der Dialog einen neuen an, mit Arzt ändert er ihn.
@Component({
  selector: 'app-arzt-dialog',
  imports: [ReactiveFormsModule, MatButtonModule, MatDialogModule, MatFormFieldModule, MatInputModule],
  templateUrl: './arzt-dialog.html',
})
export class ArztDialog {
  private readonly arztService = inject(ArztService);
  private readonly dialogRef = inject<MatDialogRef<ArztDialog, ArztResponse>>(MatDialogRef);
  protected readonly arzt: ArztResponse | null = inject(MAT_DIALOG_DATA);

  protected readonly form = inject(NonNullableFormBuilder).group({
    name: [this.arzt?.name ?? '', [Validators.required, Validators.maxLength(100)]],
    fachrichtung: [this.arzt?.fachrichtung ?? '', [Validators.required, Validators.maxLength(100)]],
  });
  protected readonly fehler = signal<Fehlermeldung | null>(null);
  protected readonly sendet = signal(false);

  protected speichern(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.sendet.set(true);
    this.fehler.set(null);
    const request = this.form.getRawValue();
    const anfrage: Observable<ArztResponse> =
      this.arzt === null ? this.arztService.anlegen(request) : this.arztService.aendern(this.arzt.id, request);
    anfrage.subscribe({
      next: (arzt) => this.dialogRef.close(arzt),
      error: (error: unknown) => {
        this.fehler.set(leseFehler(error));
        this.sendet.set(false);
      },
    });
  }
}
