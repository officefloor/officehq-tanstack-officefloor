package net.officefloor.hq.app.claim;

import java.time.LocalDate;

/** A deferral with its date parsed. */
public record ValidDefer(String user, LocalDate until) {
}
