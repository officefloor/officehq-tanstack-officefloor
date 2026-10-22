package net.officefloor.hq.app.claim;

import java.time.LocalDate;

/** A validated deferral: who is making it and the date the claim is deferred until. */
public record DeferInput(String user, LocalDate until) {
}
