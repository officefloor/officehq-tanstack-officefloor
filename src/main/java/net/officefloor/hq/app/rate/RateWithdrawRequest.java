package net.officefloor.hq.app.rate;

/** A rate withdrawal as submitted: the grade and the from-date of its rate (blank: the one from the start). */
public record RateWithdrawRequest(String grade, String from) {
}
