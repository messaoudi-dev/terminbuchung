import { DatePipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatTableModule } from '@angular/material/table';
import { AuthService } from '../../auth/auth.service';
import { BestaetigungsDaten, BestaetigungsDialog } from '../../common/bestaetigungs-dialog/bestaetigungs-dialog';
import { Fehlermeldung, leseFehler } from '../../common/problem-detail';
import { TerminResponse } from '../termin.models';
import { TerminService } from '../termin.service';

@Component({
  selector: 'app-termine',
  imports: [DatePipe, MatButtonModule, MatProgressBarModule, MatTableModule],
  templateUrl: './termine.html',
})
export class Termine implements OnInit {
  private readonly terminService = inject(TerminService);
  private readonly dialog = inject(MatDialog);
  private readonly snackBar = inject(MatSnackBar);

  protected readonly istPraxis = inject(AuthService).rolle() === 'PRAXIS';
  protected readonly spalten = this.istPraxis
    ? ['datum', 'uhrzeit', 'arzt', 'patient', 'status', 'aktionen']
    : ['datum', 'uhrzeit', 'arzt', 'status', 'aktionen'];
  protected readonly termine = signal<TerminResponse[]>([]);
  protected readonly laedt = signal(true);
  protected readonly fehler = signal<Fehlermeldung | null>(null);

  ngOnInit(): void {
    this.laden();
  }

  protected stornieren(termin: TerminResponse): void {
    const daten: BestaetigungsDaten = {
      titel: 'Termin stornieren',
      text: `Soll der Termin bei ${termin.arztName} wirklich storniert werden?`,
      bestaetigen: 'Stornieren',
    };
    this.dialog
      .open(BestaetigungsDialog, { data: daten })
      .afterClosed()
      .subscribe((bestaetigt) => {
        if (bestaetigt !== true) {
          return;
        }
        this.fehler.set(null);
        this.terminService.stornieren(termin.id).subscribe({
          next: () => {
            this.snackBar.open('Termin storniert.', 'OK', { duration: 3000 });
            this.laden();
          },
          error: (error: unknown) => this.fehler.set(leseFehler(error)),
        });
      });
  }

  private laden(): void {
    this.laedt.set(true);
    this.terminService.findeAlle().subscribe({
      next: (termine) => {
        this.termine.set(termine);
        this.laedt.set(false);
      },
      error: (error: unknown) => {
        this.fehler.set(leseFehler(error));
        this.laedt.set(false);
      },
    });
  }
}
