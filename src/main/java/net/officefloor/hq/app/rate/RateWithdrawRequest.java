package net.officefloor.hq.app.rate;

/** Body of a rate withdrawal: the grade, the from-date (yyyy-mm-dd, blank for the one with none) and who
 *  withdraws it. */
public record RateWithdrawRequest(String grade, String from, String user) {
}
