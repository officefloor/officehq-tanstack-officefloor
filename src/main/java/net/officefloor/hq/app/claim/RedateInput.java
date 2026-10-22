package net.officefloor.hq.app.claim;

import java.time.LocalDate;

/** A validated re-date: who is making it and the claim's new date. */
public record RedateInput(String user, LocalDate date) {
}
