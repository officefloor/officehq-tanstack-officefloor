package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import net.officefloor.hq.app.person.People;
import net.officefloor.hq.app.sheet.Sheets;
import net.officefloor.plugin.variable.Val;

/** Shares a claim with another person on its sheet. Refused: a SETTLED, VOID or DELETED claim, the
 *  claim's own person, a person already holding a share of it, or shares that would total more than 100.00. */
public class ApplyClaimShare {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    public void apply(@Val ClaimShareChange change, @Val Claim claim, People people, Sheets sheets)
            throws ClaimStatusRefusedException {
        ClaimShare share = change.share();
        if (sheets.archived(claim.getSheetId())) {
            throw new ClaimStatusRefusedException("The sheet is archived: nothing on it can be changed");
        }
        ClaimStatus status = claim.getStatus();
        if (status == ClaimStatus.SETTLED || status == ClaimStatus.VOID || status == ClaimStatus.DELETED) {
            throw new ClaimStatusRefusedException("A " + status + " claim cannot be shared");
        }
        if (people.find(claim.getSheetId(), share.personId()) == null) {
            throw new ClaimStatusRefusedException("Share the claim with a person on its sheet");
        }
        if (share.personId() == claim.getPersonId()) {
            throw new ClaimStatusRefusedException("A claim cannot be shared with its own person");
        }
        if (claim.getShares().stream().anyMatch(s -> s.personId() == share.personId())) {
            throw new ClaimStatusRefusedException("That person already holds a share of the claim");
        }
        if (claim.sharedTotal().add(share.share()).compareTo(HUNDRED) > 0) {
            throw new ClaimStatusRefusedException("A claim's shares cannot total more than 100.00");
        }
        List<ClaimShare> shares = new ArrayList<>(claim.getShares());
        shares.add(share);
        claim.setShares(shares);
    }
}
