package net.officefloor.hq.app.period;

import java.time.LocalDate;

/** A validated period add: who is making it, the trimmed name, and the from and to dates (from on or before to), and the
 *  cap on each person's OPEN or PENDING claims in it (at least 1; null: none). */
public record PeriodInput(String user, String name, LocalDate from, LocalDate to, Integer cap) {
}
