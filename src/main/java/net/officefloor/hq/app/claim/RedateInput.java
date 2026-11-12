package net.officefloor.hq.app.claim;

import java.time.LocalDate;

/** A validated re-dating: the claim's new date and who re-dates it. */
public record RedateInput(LocalDate date, String user) {
}
