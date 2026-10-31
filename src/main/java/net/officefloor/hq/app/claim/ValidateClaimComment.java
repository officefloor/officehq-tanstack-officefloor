package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.sheet.InvalidSheetRequestException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

public class ValidateClaimComment {
    public void validate(@RequestBody ClaimCommentRequest request, Out<ClaimComment> body)
            throws InvalidSheetRequestException {
        String user = request == null || request.user() == null ? "" : request.user().trim();
        if (user.isEmpty()) throw new InvalidSheetRequestException("Who comments on the claim must be named");
        String text = request.text() == null ? "" : request.text().trim();
        if (text.isEmpty()) throw new InvalidSheetRequestException("A comment cannot be empty");
        body.set(new ClaimComment(text, user));
    }
}
