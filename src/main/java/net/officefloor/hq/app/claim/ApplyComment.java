package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.sheet.SheetStore;
import net.officefloor.plugin.variable.Val;

/** Keeps the comment on the claim, written on the sheet's today. */
public class ApplyComment {
    public void apply(@Val CommentInput input, @Val Claim claim, ClaimCommentStore comments, SheetStore sheets) {
        comments.add(claim, input.user(), sheets.today(claim.getSheetId()), input.text());
    }
}
