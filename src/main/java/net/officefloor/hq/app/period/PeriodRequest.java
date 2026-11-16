package net.officefloor.hq.app.period;

/** Body of adding a period: its name, from and to dates (yyyy-MM-dd), who adds it, and its cap (a whole number above 0; blank for none). */
public record PeriodRequest(String name, String from, String to, String by, String cap) {
}
