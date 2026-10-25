package net.officefloor.hq.app.person;

import java.time.LocalDate;

/** A validated hold: its from-date (null: the sheet's today) and its to-date (null: open-ended). */
public record HoldInput(LocalDate from, LocalDate to) {
}
