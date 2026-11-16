package net.officefloor.hq.app.rate;

import java.time.LocalDate;

/** A rate to withdraw: its grade and from-date (null: from the beginning of time). */
public record RateWithdrawal(String sheetId, String grade, LocalDate from) {
}
