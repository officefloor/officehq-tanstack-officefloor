package net.officefloor.hq.app.rate;

/** The rate to withdraw: its grade and from-date (yyyy-MM-dd; blank or null for the one that applies from the start). */
public record RateWithdrawRequest(String grade, String from) {
}
