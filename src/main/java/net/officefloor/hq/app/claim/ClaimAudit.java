package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.person.Person;

/** The audit line of a claim change: amount as shown (two decimals), date as yyyy-mm-dd. */
final class ClaimAudit {

    private ClaimAudit() {
    }

    static String line(String event, String user, Person person, Claim claim) {
        ClaimResponse shown = ClaimResponse.from(claim);
        return event + " sheet=" + claim.getSheetId() + " by=" + user + " person=" + person.getName()
                + " amount=" + shown.amount() + " date=" + shown.date();
    }
}
