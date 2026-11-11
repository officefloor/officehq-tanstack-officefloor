package net.officefloor.hq.app.period;

/**
 * A period as the screen sends it: a name, and a from and to date (yyyy-MM-dd; a blank from is the
 * sheet's today), and a cap (a whole number, at least 1; blank for none).
 */
public record PeriodRequest(String name, String from, String to, String cap) {
}
