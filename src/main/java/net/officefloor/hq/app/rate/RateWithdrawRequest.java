package net.officefloor.hq.app.rate;

/** A rate to withdraw: its grade and its from-date (blank or absent: from the beginning of time). */
public record RateWithdrawRequest(String grade, String from) {
}
