package net.officefloor.hq.app.claim;

import java.util.List;
import net.officefloor.plugin.variable.Val;

/** Refuses (400) an edit typing another amount than a claim with lines has: its lines make its amount. */
public class CheckClaimLinesAmount {
    public void check(@Val Claim claim, @Val ClaimInput input, ClaimLineRepository lines) throws InvalidClaimException {
        List<ClaimLine> found = lines.find(claim.sheetId(), claim.id());
        if (!found.isEmpty() && input.amount().compareTo(ClaimLineRepository.total(found)) != 0) {
            throw new InvalidClaimException("The claim's lines make its amount " + ClaimLineRepository.total(found) + ".");
        }
    }
}
