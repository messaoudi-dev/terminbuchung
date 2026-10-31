import { Component, OnInit, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatTableModule } from '@angular/material/table';
import { AuthService } from '../../auth/auth.service';
import { BestaetigungsDaten, BestaetigungsDialog } from '../../common/bestaetigungs-dialog/bestaetigungs-dialog';
import { Fehlermeldung, leseFehler } from '../../common/problem-detail';
import { ArztDialog } from '../arzt-dialog/arzt-dialog';
import { ArztResponse } from '../arzt.models';
import { ArztService } from '../arzt.service';

@Component({
  selector: 'app-aerzte',
  imports: [MatButtonModule, MatProgressBarModule, MatTableModule],
  templateUrl: './aerzte.html',
})
export class Aerzte implements OnInit {
  private readonly arztService = inject(ArztService);
  private readonly dialog = inject(MatDialog);
  private readonly snackBar = inject(MatSnackBar);

  protected readonly istPraxis = inject(AuthService).rolle() === 'PRAXIS';
  protected readonly spalten = this.istPraxis ? ['name', 'fachrichtung', 'aktionen'] : ['name', 'fachrichtung'];
  protected readonly aerzte = signal<ArztResponse[]>([]);
  protected readonly laedt = signal(true);
  protected readonly fehler = signal<Fehlermeldung | null>(null);

  ngOnInit(): void {
    this.laden();
  }

  protected anlegen(): void {
    this.oeffneDialog(null, 'Arzt angelegt.');
  }

  protected aendern(arzt: ArztResponse): void {
    this.oeffneDialog(arzt, 'Arzt geändert.');
  }

  protected loeschen(arzt: ArztResponse): void {
    const daten: BestaetigungsDaten = {
      titel: 'Arzt löschen',
      text: `Soll ${arzt.name} wirklich gelöscht werden?`,
      bestaetigen: 'Löschen',
    };
    this.dialog
      .open(BestaetigungsDialog, { data: daten })
      .afterClosed()
      .subscribe((bestaetigt) => {
        if (bestaetigt !== true) {
          return;
        }
        this.fehler.set(null);
        this.arztService.loeschen(arzt.id).subscribe({
          next: () => {
            this.snackBar.open('Arzt gelöscht.', 'OK', { duration: 3000 });
            this.laden();
          },
          error: (error: unknown) => this.fehler.set(leseFehler(error)),
        });
      });
  }

  private oeffneDialog(arzt: ArztResponse | null, erfolgsmeldung: string): void {
    this.dialog
      .open(ArztDialog, { data: arzt, width: '420px' })
      .afterClosed()
      .subscribe((gespeichert) => {
        if (gespeichert === undefined) {
          return;
        }
        this.snackBar.open(erfolgsmeldung, 'OK', { duration: 3000 });
        this.laden();
      });
  }

  private laden(): void {
    this.laedt.set(true);
    this.arztService.findeAlle().subscribe({
      next: (aerzte) => {
        this.aerzte.set(aerzte);
        this.laedt.set(false);
      },
      error: (error: unknown) => {
        this.fehler.set(leseFehler(error));
        this.laedt.set(false);
      },
    });
  }
}
