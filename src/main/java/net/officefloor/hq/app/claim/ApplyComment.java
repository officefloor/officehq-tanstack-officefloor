package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.session.ChangedBy;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Val;

/** Keeps the comment on the claim, with the user who wrote it and the sheet's today. */
public class ApplyComment {
    public void apply(@Val Claim claim, @Val ClaimComment comment, @Val ChangedBy by, ClaimCommentRepository comments,
            SheetRepository sheets) {
        comments.add(claim, by.name(), sheets.today(claim.getSheetId()), comment.text());
    }
}
