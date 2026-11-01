package net.officefloor.hq.app.sheet;

import java.time.LocalDate;

/** The sheet's today before a new one took effect: the day being left. */
public record LeftDay(LocalDate day) {
}
