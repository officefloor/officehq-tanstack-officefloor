package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestParam;

/** Keeps the comment on the claim, with the user who wrote it and the sheet's today. */
public class AddClaimComment {
    public void add(@Val Claim claim, @Val CommentText text, @RequestParam("user") String user,
            SheetRepository sheets, ClaimCommentRepository comments, Out<ClaimComment> added) {
        ClaimComment comment = new ClaimComment(claim.id(), user, sheets.today(claim.sheetId()), text.text());
        comments.append(claim.sheetId(), comment);
        added.set(comment);
    }
}
