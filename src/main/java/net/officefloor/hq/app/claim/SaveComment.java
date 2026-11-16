package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Val;

/** Stores the comment on the claim, dated the sheet's today. */
public class SaveComment {
    public void save(@Val Claim claim, @Val ClaimComment comment, ClaimRepository claims, SheetRepository sheets) {
        claims.comment(claim, new ClaimComment(comment.by(), sheets.today(claim.sheetId()), comment.text()));
    }
}
