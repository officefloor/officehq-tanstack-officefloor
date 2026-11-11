package net.officefloor.hq.app.claim;

import java.time.LocalDate;

/** A claim's due-by set directly: the due-by it had and the one it has now. */
public record DueBySetting(LocalDate from, LocalDate to) {
}
