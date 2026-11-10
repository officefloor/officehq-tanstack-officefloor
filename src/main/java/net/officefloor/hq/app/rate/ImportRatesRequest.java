package net.officefloor.hq.app.rate;

/** Rates pasted into the rate import box: a rate per line of grade,rate,from,to. */
public record ImportRatesRequest(String text) {
}
