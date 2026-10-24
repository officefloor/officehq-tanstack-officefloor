package net.officefloor.hq.app.person;

import java.time.LocalDate;

/** A validated hold request: the from-date (null: the sheet's today) and the to-date (null: open-ended). */
public record HoldInput(LocalDate from, LocalDate to) {
}
