package net.officefloor.hq.app.claim;

import java.time.LocalDate;

/** A stored due-by setting: the due-by the claim had and the due-by it has now. */
public record DueBySetting(LocalDate old, LocalDate now) {
}
