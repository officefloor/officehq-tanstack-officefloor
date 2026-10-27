package net.officefloor.hq.app.rollup;

import java.time.LocalDate;

/** The sheet's today before it is changed: the day being left. */
public record TodayLeft(LocalDate today) {
}
