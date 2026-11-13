package net.officefloor.hq.app.rate;

/** Body of POST /api/sheets/{sheetId}/rates/withdraw: the grade, the date its rate applies from (blank for
 *  from the beginning of time) and who withdraws it. */
public record RateWithdrawRequest(String grade, String from, String user) {
}
