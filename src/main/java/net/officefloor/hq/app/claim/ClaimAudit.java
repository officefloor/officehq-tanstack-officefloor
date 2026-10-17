package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.person.PersonRepository;

/** The audit line for a saved claim: the event, sheet and user, then the person's name, the amount
 *  (two decimals, as shown) and the date (yyyy-mm-dd); then, when the claim has a number, number=. */
final class ClaimAudit {
    private ClaimAudit() {
    }

    static String line(String event, Claim claim, String user, PersonRepository people) {
        Integer number = claim.getNumber();
        String person = people.findById(claim.getPersonId()).orElseThrow().getName();
        ClaimResponse shown = ClaimResponse.from(claim);
        return event + " sheet=" + claim.getSheetId() + " by=" + (user == null ? "" : user.trim())
                + " person=" + person + " amount=" + shown.amount() + " date=" + shown.date()
                + (number == null ? "" : " number=" + number);
    }
}
