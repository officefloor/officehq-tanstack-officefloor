package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.person.Person;

/**
 * The audit line of a claim change: amount as shown (two decimals), date as yyyy-mm-dd, then the
 * claim's number when it has one.
 */
final class ClaimAudit {

    private ClaimAudit() {
    }

    /** CLAIM_ADDED: the line, then ref=<reference> when the claim carries one. */
    static String added(String user, Person person, Claim claim) {
        return line("CLAIM_ADDED", user, person, claim, claim.getNumber()) + (claim.getRef() == null ? "" : " ref=" + claim.getRef());
    }

    static String line(String event, String user, Person person, Claim claim, String number) {
        ClaimResponse shown = ClaimResponse.from(claim);
        return event + " sheet=" + claim.getSheetId() + " by=" + user + " person=" + person.getName()
                + " amount=" + shown.amount() + " date=" + shown.date() + (number == null ? "" : " number=" + number);
    }
}
