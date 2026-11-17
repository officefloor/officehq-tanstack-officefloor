package net.officefloor.hq.app.rate;

/** Body of withdrawing a grade's rate: its from-date (yyyy-MM-dd, blank for none) and who withdraws it. */
public record RateWithdrawRequest(String from, String by) {
}
