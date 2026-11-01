package net.officefloor.hq.app.rate;

import java.time.LocalDate;

/** The newest version of a grade's rate for a from-date (null: from the beginning of time) to withdraw. */
public record RateWithdrawal(String sheetId, String grade, LocalDate from) {
}
