package net.officefloor.hq.app.period;

/** Body of an openings paste: lines of name,period,amount as typed, and who sets them. */
public record ImportOpeningsRequest(String text, String user) {
}
