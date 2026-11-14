package net.officefloor.hq.app.rate;

/** A withdrawn rate as the screen is told it: grade, from-date (null: from the start), version now in force (0: gone). */
public record RateWithdrawnResponse(String grade, String from, int version) {
}
