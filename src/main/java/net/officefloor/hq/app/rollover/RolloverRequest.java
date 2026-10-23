package net.officefloor.hq.app.rollover;

/** Body of a rollover: who is making it, and the old fiscal year (yyyy: FY<yyyy> is rolled over into the next). */
public record RolloverRequest(String user, String year) {
}
