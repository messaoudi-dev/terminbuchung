import { Component, inject, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { Router, RouterLink } from '@angular/router';
import { Fehlermeldung, leseFehler } from '../../common/problem-detail';
import { AuthService } from '../auth.service';

@Component({
  selector: 'app-registrierung',
  imports: [
    ReactiveFormsModule,
    RouterLink,
    MatButtonModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
  ],
  templateUrl: './registrierung.html',
})
export class Registrierung {
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  // Passwortlänge prüft nur das Backend; das Frontend prüft nur Pflichtfelder und E-Mail-Format.
  protected readonly form = inject(NonNullableFormBuilder).group({
    email: ['', [Validators.required, Validators.email]],
    passwort: ['', Validators.required],
  });
  protected readonly fehler = signal<Fehlermeldung | null>(null);
  protected readonly sendet = signal(false);

  protected registrieren(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.sendet.set(true);
    this.fehler.set(null);
    this.authService.registrieren(this.form.getRawValue()).subscribe({
      next: () => this.router.navigate(['/login'], { queryParams: { registriert: 'true' } }),
      error: (error: unknown) => {
        this.fehler.set(leseFehler(error));
        this.sendet.set(false);
      },
    });
  }
}
