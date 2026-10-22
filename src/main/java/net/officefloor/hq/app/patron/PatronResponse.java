package net.officefloor.hq.app.patron;

/** A person's patron as the screen gets it: whose patron, and who (by id). */
public record PatronResponse(long personId, long patronId) {
    public static PatronResponse from(Patron p) {
        return new PatronResponse(p.personId(), p.patronId());
    }
}
