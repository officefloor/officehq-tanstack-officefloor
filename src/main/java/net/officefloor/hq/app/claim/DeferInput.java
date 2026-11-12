package net.officefloor.hq.app.claim;

import java.time.LocalDate;

/** A validated deferral: the date the claim is deferred until and who defers it. */
public record DeferInput(LocalDate until, String user) {
}
