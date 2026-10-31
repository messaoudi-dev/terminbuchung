import { Component, inject } from '@angular/core';
import { ActivatedRoute } from '@angular/router';

@Component({
  selector: 'app-platzhalter',
  template: `
    <h1>{{ titel }}</h1>
    <p>Diese Seite ist noch in Arbeit.</p>
  `,
})
export class Platzhalter {
  protected readonly titel: string = inject(ActivatedRoute).snapshot.data['titel'];
}
