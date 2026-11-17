package net.officefloor.hq.app.rate;

import java.time.LocalDate;

/** A rate withdrawal once validated: the grade, its from-date (null for none), and who withdraws it. */
public record RateWithdrawValues(String grade, LocalDate from, String by) {
}
