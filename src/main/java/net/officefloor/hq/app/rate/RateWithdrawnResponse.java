package net.officefloor.hq.app.rate;

/** A withdrawn rate as the screen sees it: its grade and from-date (null: from the beginning of time), and the version that applies again (null: none). */
public record RateWithdrawnResponse(String grade, String from, RateResponse restored) {
}
