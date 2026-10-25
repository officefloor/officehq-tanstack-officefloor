package net.officefloor.hq.app.claim;

import java.time.LocalDate;

/** The date a claim is to be deferred until. */
public record DeferUntil(LocalDate date) {
}
