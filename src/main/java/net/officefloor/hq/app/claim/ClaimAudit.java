package net.officefloor.hq.app.claim;

/** A claim's audit line: the event, sheet, user, person, amount as the screen shows it, date, and the claim's
 * number when it has one. */
public final class ClaimAudit {

    private ClaimAudit() {
    }

    public static String line(String event, Claim claim, String person, String user) {
        return line(event, claim, person, user, claim.getNumber());
    }

    static String line(String event, Claim claim, String person, String user, String number) {
        return event + " sheet=" + claim.getSheetId() + " by=" + user + " person=" + person
                + " amount=" + claim.getAmount().setScale(2).toPlainString() + " date=" + claim.getDate()
                + (number == null ? "" : " number=" + number);
    }
}
