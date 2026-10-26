package net.officefloor.hq.app.rate;

/** The rate import box as typed: lines of grade,rate,from,to, and the signed-in user (`by`) making the change. */
public record ImportRatesRequest(String lines, String by) {
}
