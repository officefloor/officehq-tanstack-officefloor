package net.officefloor.hq.app.period;

/** Body of a new period: its name, its from and to dates (yyyy-mm-dd), its cap (a whole number, blank
 *  for none) and who adds it. */
public record PeriodRequest(String name, String from, String to, String cap, String user) {
}
