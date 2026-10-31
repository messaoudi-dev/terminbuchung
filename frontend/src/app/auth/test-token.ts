// Baut ein JWT mit beliebigen Claims für Tests. Die Signatur ist egal, weil das Frontend sie nicht prüft.
export function erzeugeTestToken(claims: object): string {
  const payload = btoa(JSON.stringify(claims))
    .replace(/=+$/, '')
    .replace(/\+/g, '-')
    .replace(/\//g, '_');
  return `eyJhbGciOiJIUzI1NiJ9.${payload}.signatur`;
}

export const JETZT = new Date('2026-11-02T10:00:00Z');
export const IN_EINER_STUNDE = JETZT.getTime() / 1000 + 3600;
