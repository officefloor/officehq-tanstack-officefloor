package net.officefloor.hq.app.rate;

/** Body of withdrawing a rate's newest version: its grade and from-date (blank: from the beginning of time). */
public record WithdrawRateRequest(String grade, String from) {
}
