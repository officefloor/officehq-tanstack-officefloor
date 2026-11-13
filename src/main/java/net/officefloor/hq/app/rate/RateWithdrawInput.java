package net.officefloor.hq.app.rate;

import java.time.LocalDate;

/** A validated rate withdrawal: the grade, the date its rate applies from (null for from the beginning of
 *  time) and who withdraws it. */
public record RateWithdrawInput(String grade, LocalDate from, String user) {
}
