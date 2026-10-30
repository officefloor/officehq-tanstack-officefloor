package net.officefloor.hq.app.claim;

import java.util.ArrayList;
import java.util.List;
import net.officefloor.hq.app.period.Periods;
import net.officefloor.plugin.variable.Val;

/** Adding a line counts as editing the claim: only a DRAFT outside any closed period, not while another user's editor has it
 *  open. The claim's amount becomes its lines' amounts added up. */
public class ApplyLine {
    public void apply(@Val LineRequest request, @Val ClaimLine line, @Val Claim claim, Periods periods, Claims claims)
            throws InvalidClaimException {
        if (claim.getStatus() != ClaimStatus.DRAFT) {
            throw new InvalidClaimException("Only a DRAFT claim can have lines added");
        }
        String lockedBy = claims.lockedBy(claim);
        if (lockedBy != null && !lockedBy.equals(request.user())) {
            throw new InvalidClaimException("The claim is being edited by " + lockedBy);
        }
        if (periods.isClosed(claim.getSheetId(), claim.getPeriodDate())) {
            throw new InvalidClaimException("No claim whose due-by is inside a closed period can be edited");
        }
        List<ClaimLine> lines = new ArrayList<>(claims.lines(claim));
        lines.add(line);
        claim.setLines(lines);
        claim.setAmount(ClaimLine.sum(lines));
        claim.setEditedBy(request.user());
    }
}
