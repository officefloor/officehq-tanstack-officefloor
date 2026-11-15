package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestParam;

/** Comments a claim: by the user, on the sheet's today. Refused for an empty comment. */
public class CommentClaim {
    public void comment(@RequestParam(name = "by", required = false) String by,
            @RequestParam(name = "text", required = false) String text, @Val Claim claim,
            SheetRepository sheets, Out<ClaimComment> comment) throws InvalidClaimException {
        if (text == null || text.isBlank()) {
            throw new InvalidClaimException("A comment cannot be empty");
        }
        comment.set(new ClaimComment(by == null ? "" : by, sheets.today(claim.getSheetId()).toString(), text.trim()));
    }
}
