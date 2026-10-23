package net.officefloor.hq.app.period;

/** Body of a period add: who is making it, the name, the from and to dates (yyyy-mm-dd), and the cap on each person's
 *  OPEN or PENDING claims in it (a whole number; blank or absent: none). */
public record PeriodRequest(String user, String name, String from, String to, String cap) {
}
