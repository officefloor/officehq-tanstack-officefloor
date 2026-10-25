package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.session.ChangedBy;

/** A claim's audit record: the event, sheet, user, then person, amount (two decimals) and date. */
final class ClaimAudit {

    private ClaimAudit() {
    }

    static String line(String event, Claim claim, ChangedBy by, String personName) {
        return event + " sheet=" + claim.getSheetId() + " by=" + by.name() + " person=" + personName + " amount="
                + claim.getAmount().setScale(2).toPlainString() + " date=" + claim.getDate();
    }
}
