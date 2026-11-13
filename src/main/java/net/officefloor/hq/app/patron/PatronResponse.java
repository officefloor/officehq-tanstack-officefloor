package net.officefloor.hq.app.patron;

/** A person and one of their patrons, as the screen reads them. */
public record PatronResponse(long personId, long patronId) {

    public static PatronResponse from(Patron patron) {
        return new PatronResponse(patron.personId(), patron.patronId());
    }
}
