// Das Backend erwartet LocalDateTime ohne Zeitzone. toISOString() würde nach UTC umrechnen
// und z. B. aus 09:30 Uhr in Berlin 08:30 Uhr machen.
export function zuLokalemZeitstring(datum: Date, uhrzeit: string): string {
  const jahr = datum.getFullYear();
  const monat = zweistellig(datum.getMonth() + 1);
  const tag = zweistellig(datum.getDate());
  return `${jahr}-${monat}-${tag}T${uhrzeit}:00`;
}

// Nur Bedienkomfort: Die Öffnungszeiten prüft das Backend.
export function uhrzeitRaster(): string[] {
  const uhrzeiten: string[] = [];
  for (let stunde = 8; stunde < 18; stunde++) {
    uhrzeiten.push(`${zweistellig(stunde)}:00`);
    uhrzeiten.push(`${zweistellig(stunde)}:30`);
  }
  return uhrzeiten;
}

function zweistellig(zahl: number): string {
  return String(zahl).padStart(2, '0');
}
