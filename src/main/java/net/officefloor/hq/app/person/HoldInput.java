package net.officefloor.hq.app.person;

import java.time.LocalDate;

/** A validated hold: its from-date (null: the sheet's today) and to-date (null: none). */
public record HoldInput(LocalDate from, LocalDate to) {
}
