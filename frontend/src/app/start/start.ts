import { Component, inject } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { RouterLink } from '@angular/router';
import { AuthService } from '../auth/auth.service';

@Component({
  selector: 'app-start',
  imports: [RouterLink, MatButtonModule, MatCardModule],
  templateUrl: './start.html',
  styleUrl: './start.scss',
})
export class Start {
  protected readonly authService = inject(AuthService);
}
