import { Component, inject, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { Fehlermeldung, leseFehler } from '../../common/problem-detail';
import { AuthService } from '../auth.service';

@Component({
  selector: 'app-login',
  imports: [
    ReactiveFormsModule,
    RouterLink,
    MatButtonModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
  ],
  templateUrl: './login.html',
})
export class Login {
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  protected readonly form = inject(NonNullableFormBuilder).group({
    email: ['', [Validators.required, Validators.email]],
    passwort: ['', Validators.required],
  });
  protected readonly registriert = inject(ActivatedRoute).snapshot.queryParamMap.has('registriert');
  protected readonly fehler = signal<Fehlermeldung | null>(null);
  protected readonly sendet = signal(false);

  protected anmelden(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.sendet.set(true);
    this.fehler.set(null);
    this.authService.login(this.form.getRawValue()).subscribe({
      next: () => this.router.navigate(['/start']),
      error: (error: unknown) => {
        this.fehler.set(leseFehler(error));
        this.sendet.set(false);
      },
    });
  }
}
