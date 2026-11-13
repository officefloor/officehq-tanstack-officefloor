package net.officefloor.hq.app.claim;

import java.util.List;
import net.officefloor.hq.app.sheet.InvalidSheetException;
import net.officefloor.plugin.variable.Val;

/** Stores the claim shared with the person at their share, after its other holders; refused when another user
 *  changed its shares or moved it on first. */
public class ApplyShare {
    public void apply(@Val ShareInput input, @Val Claim claim, ClaimStore store) throws InvalidSheetException {
        List<Shares.Share> shares = Shares.list(claim.getShares());
        shares.add(new Shares.Share(input.personId(), input.share()));
        if (!store.share(claim, Shares.stored(shares))) {
            throw new InvalidSheetException("The claim was changed meanwhile; load the sheet again");
        }
    }
}
