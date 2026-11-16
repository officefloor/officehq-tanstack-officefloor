package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Val;

/** Stores the line, dated the sheet's today, and the claim's new amount as an edit by who added it; refused when the
 *  claim is no longer a DRAFT or another user holds its lock (has its editor open). */
public class SaveLine {
    public void save(@Val Claim claim, @Val ClaimLine line, ClaimRepository claims, SheetRepository sheets)
            throws InvalidClaimException {
        if (!claims.addLine(claim, new ClaimLine(line.by(), sheets.today(claim.sheetId()), line.label(), line.qty(), line.unit()))) {
            throw new InvalidClaimException("Only a DRAFT claim no one else is editing can have lines added");
        }
    }
}
