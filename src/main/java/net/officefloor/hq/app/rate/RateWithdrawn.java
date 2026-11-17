package net.officefloor.hq.app.rate;

import java.time.LocalDate;

/** What a withdrawal left: the grade's rate for the from-date that applies again, or null when the rate is gone. */
public record RateWithdrawn(String sheetId, String grade, LocalDate from, Rate restored) {
}
