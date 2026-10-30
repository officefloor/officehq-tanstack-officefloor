package net.officefloor.hq.app.claim;

import java.time.LocalDate;

/** A re-dating with its new date parsed. */
public record ValidRedate(String user, LocalDate date) {
}
