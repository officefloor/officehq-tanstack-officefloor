package net.officefloor.hq.app.period;

import java.time.LocalDate;

/** A validated period to add: its trimmed name, its from and to dates (inclusive), who adds it and the most OPEN or
 *  PENDING claims a person may have in it (null for no cap). */
public record PeriodInput(String name, LocalDate from, LocalDate to, String user, Integer cap) {
}
