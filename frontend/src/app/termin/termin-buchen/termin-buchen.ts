import { Component, OnInit, inject, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Router } from '@angular/router';
import { ArztResponse } from '../../arzt/arzt.models';
import { ArztService } from '../../arzt/arzt.service';
import { Fehlermeldung, leseFehler } from '../../common/problem-detail';
import { uhrzeitRaster, zuLokalemZeitstring } from '../lokale-zeit';
import { TerminService } from '../termin.service';

@Component({
  selector: 'app-termin-buchen',
  imports: [
    ReactiveFormsModule,
    MatButtonModule,
    MatCardModule,
    MatDatepickerModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
  ],
  templateUrl: './termin-buchen.html',
})
export class TerminBuchen implements OnInit {
  private readonly arztService = inject(ArztService);
  private readonly terminService = inject(TerminService);
  private readonly snackBar = inject(MatSnackBar);
  private readonly router = inject(Router);

  protected readonly form = inject(NonNullableFormBuilder).group({
    arztId: [null as number | null, Validators.required],
    datum: [null as Date | null, Validators.required],
    uhrzeit: ['', Validators.required],
  });
  protected readonly heute = new Date();
  protected readonly uhrzeiten = uhrzeitRaster();
  protected readonly aerzte = signal<ArztResponse[]>([]);
  protected readonly fehler = signal<Fehlermeldung | null>(null);
  protected readonly sendet = signal(false);

  ngOnInit(): void {
    this.arztService.findeAlle().subscribe({
      next: (aerzte) => this.aerzte.set(aerzte),
      error: (error: unknown) => this.fehler.set(leseFehler(error)),
    });
  }

  protected buchen(): void {
    const { arztId, datum, uhrzeit } = this.form.getRawValue();
    if (this.form.invalid || arztId === null || datum === null) {
      this.form.markAllAsTouched();
      return;
    }
    this.sendet.set(true);
    this.fehler.set(null);
    this.terminService.buchen({ arztId, beginn: zuLokalemZeitstring(datum, uhrzeit) }).subscribe({
      next: () => {
        this.snackBar.open('Termin gebucht.', 'OK', { duration: 3000 });
        this.router.navigate(['/termine']);
      },
      error: (error: unknown) => {
        this.fehler.set(leseFehler(error));
        this.sendet.set(false);
      },
    });
  }
}
