package net.officefloor.hq.app.rate;

import java.time.LocalDate;

/** A validated rate withdrawal: the grade and the from-date of its rate (null: the one from the start). */
public record RateWithdrawal(String grade, LocalDate from) {
}
