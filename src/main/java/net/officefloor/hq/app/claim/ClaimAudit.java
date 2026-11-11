package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.sheet.Sheet;

/**
 * A claim's audit record: its person, its amount as the screen shows it, its date, and its number
 * when it has one (a voided claim keeps the number it had).
 */
final class ClaimAudit {
    private ClaimAudit() {}

    static String line(String event, Sheet sheet, String user, Person person, Claim claim) {
        return unnumbered(event, sheet, user, person, claim)
                + (claim.getNumber() == null ? "" : " number=" + claim.getNumber());
    }

    /** The claim's audit record without its number. */
    static String unnumbered(String event, Sheet sheet, String user, Person person, Claim claim) {
        ClaimResponse shown = ClaimResponse.from(claim);
        return event + " sheet=" + sheet.id() + " by=" + user + " person=" + person.getName()
                + " amount=" + shown.amount() + " date=" + shown.date();
    }
}
