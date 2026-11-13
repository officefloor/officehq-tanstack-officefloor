package net.officefloor.hq.app.claim;

import java.time.LocalDate;

/** A claim's deferral: the date it is deferred until. */
public record Deferral(LocalDate until) {
}
