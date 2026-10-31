package net.officefloor.hq.app.person;

import java.time.LocalDate;

/** A checked retirement: the date retired from (null: the sheet's today) and by whom. */
public record Retirement(LocalDate from, String user) {
}
