package net.officefloor.hq.app.rate;

/** The body of a rate's withdrawal: the from-date of the rate withdrawn (blank: the one from the beginning of time). */
public record RateWithdrawRequest(String from) {
}
