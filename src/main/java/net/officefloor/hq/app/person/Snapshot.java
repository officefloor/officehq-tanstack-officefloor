package net.officefloor.hq.app.person;

import java.time.LocalDate;

/** A SNAPSHOT written for a person: the day being left when the sheet's today was changed, and their due-total then ("n/a" when they had none). */
public record Snapshot(LocalDate day, String dueTotal) {
}
