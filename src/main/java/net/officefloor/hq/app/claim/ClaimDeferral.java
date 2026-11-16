package net.officefloor.hq.app.claim;

import java.time.LocalDate;

/** A deferral once its body is read: the date the claim is deferred until, and who defers it. */
public record ClaimDeferral(LocalDate until, String by) {
}
