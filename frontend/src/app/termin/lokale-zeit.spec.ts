import { uhrzeitRaster, zuLokalemZeitstring } from './lokale-zeit';

describe('zuLokalemZeitstring', () => {
  it('zuLokalemZeitstring_datumUndUhrzeit_liefertLokalenZeitstring', () => {
    // Arrange
    const datum = new Date(2026, 10, 12);

    // Act
    const ergebnis = zuLokalemZeitstring(datum, '09:30');

    // Assert
    expect(ergebnis).toBe('2026-11-12T09:30:00');
  });

  it('zuLokalemZeitstring_einstelligerMonatUndTag_fuelltMitNullAuf', () => {
    // Arrange
    const datum = new Date(2027, 0, 4);

    // Act
    const ergebnis = zuLokalemZeitstring(datum, '17:30');

    // Assert
    expect(ergebnis).toBe('2027-01-04T17:30:00');
  });

  it('zuLokalemZeitstring_datumKurzNachMitternacht_bleibtAmSelbenTag', () => {
    // Arrange: Der Datepicker liefert Mitternacht in lokaler Zeit; in UTC wäre das östlich von Greenwich der Vortag.
    const datum = new Date(2026, 10, 2, 0, 15);

    // Act
    const ergebnis = zuLokalemZeitstring(datum, '08:00');

    // Assert
    expect(ergebnis).toBe('2026-11-02T08:00:00');
  });
});

describe('uhrzeitRaster', () => {
  it('uhrzeitRaster_oeffnungszeiten_liefert30MinutenRasterVon8Bis1730', () => {
    // Act
    const raster = uhrzeitRaster();

    // Assert
    expect(raster).toHaveLength(20);
    expect(raster[0]).toBe('08:00');
    expect(raster[1]).toBe('08:30');
    expect(raster[raster.length - 1]).toBe('17:30');
    expect(raster).not.toContain('18:00');
  });
});
