package net.officefloor.hq.app.person;

/** One of a person's past retirements: its from-date and the day they were unretired (yyyy-mm-dd). */
public record RetirementResponse(String from, String to) {
    public static RetirementResponse from(RetirementSpan span) {
        return new RetirementResponse(span.from().toString(), span.to().toString());
    }
}
