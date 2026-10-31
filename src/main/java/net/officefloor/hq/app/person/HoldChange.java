package net.officefloor.hq.app.person;

import java.time.LocalDate;

/** A checked hold: the date it is from (null: the sheet's today), to (null: open-ended) and by whom. */
public record HoldChange(LocalDate from, LocalDate to, String user) {
}
