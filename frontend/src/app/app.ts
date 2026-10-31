import { Component, inject } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatToolbarModule } from '@angular/material/toolbar';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService } from './auth/auth.service';

@Component({
  imports: [RouterOutlet, RouterLink, RouterLinkActive, MatButtonModule, MatToolbarModule],
  selector: 'app-root',
  styleUrl: './app.scss',
  templateUrl: './app.html',
})
export class App {
  protected readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  protected benutzerInfo(): string {
    const rolle = this.authService.rolle() === 'PRAXIS' ? 'Praxis' : 'Patient';
    const email = this.authService.email();
    return email === null ? rolle : `${email} · ${rolle}`;
  }

  protected abmelden(): void {
    this.authService.abmelden();
    this.router.navigate(['/login']);
  }
}
