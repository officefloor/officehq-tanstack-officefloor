package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.session.ChangedBy;

/**
 * A claim's audit record: the event, sheet, user, then person, amount (two decimals) and date, and
 * the claim's number when it has one.
 */
final class ClaimAudit {

    private ClaimAudit() {
    }

    static String line(String event, Claim claim, ChangedBy by, String personName) {
        return line(event, claim, by, personName, claim.getNumber());
    }

    /** The CLAIM_ADDED record: the claim's line, then its reference and its tags when it has them. */
    static String added(Claim claim, ChangedBy by, String personName) {
        return line("CLAIM_ADDED", claim, by, personName) + (claim.getRef() == null ? "" : " ref=" + claim.getRef())
                + (claim.getTags() == null ? "" : " tags=" + claim.getTags());
    }

    static String line(String event, Claim claim, ChangedBy by, String personName, String number) {
        return event + " sheet=" + claim.getSheetId() + " by=" + by.name() + " person=" + personName + " amount="
                + claim.getAmount().setScale(2).toPlainString() + " date=" + claim.getDate()
                + (number == null ? "" : " number=" + number);
    }
}
