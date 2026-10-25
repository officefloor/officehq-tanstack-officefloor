package net.officefloor.hq.app.person;

/** One of a person's holds: its from-date and to-date (yyyy-mm-dd; null: open at that end). */
public record HoldResponse(String from, String to) {
    public static HoldResponse from(HoldSpan hold) {
        return new HoldResponse(hold.from() == null ? null : hold.from().toString(),
                hold.to() == null ? null : hold.to().toString());
    }
}
