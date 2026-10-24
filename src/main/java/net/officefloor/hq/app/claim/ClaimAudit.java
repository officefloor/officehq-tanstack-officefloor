package net.officefloor.hq.app.claim;

/** A claim's audit line: the event, sheet, user, person, amount as the screen shows it, and date. */
final class ClaimAudit {

    private ClaimAudit() {
    }

    static String line(String event, Claim claim, String person, String user) {
        return event + " sheet=" + claim.getSheetId() + " by=" + user + " person=" + person
                + " amount=" + claim.getAmount().setScale(2).toPlainString() + " date=" + claim.getDate();
    }
}
