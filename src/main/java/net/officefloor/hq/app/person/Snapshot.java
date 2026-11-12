package net.officefloor.hq.app.person;

import java.time.LocalDate;

/** A snapshot of a person, written when the sheet's today moved on: the day being left and their due-total as
 *  it stood then (as shown: two decimals, or n/a). */
public record Snapshot(LocalDate day, String dueTotal) {
}
