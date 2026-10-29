package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.setting.Settings;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import net.officefloor.server.http.ServerHttpConnection;
import org.springframework.web.bind.annotation.RequestBody;

public class ApplyReviewAssign {
    // Only a steward (or whom the steward role is delegated to) assigns, only a REVIEW claim, and only to a user named.
    // Stored only if the claim is still in REVIEW, so a claim reviewed meanwhile by another user is refused.
    public void apply(@RequestBody(required = false) ReviewAssignRequest body, @Val Claim claim, Claims claims,
            Settings settings, ServerHttpConnection connection, Out<ClaimResponse> responseOut)
            throws ClaimReviewAssignRefusedException {
        String user = Audit.user(connection).trim();
        if (!Settings.STEWARD.equals(settings.role(claim.sheetId(), user))) {
            throw new ClaimReviewAssignRefusedException("Only a steward can assign a claim's review.");
        }
        if (claim.status() != ClaimStatus.REVIEW) {
            throw new ClaimReviewAssignRefusedException("Only a REVIEW claim's review can be assigned.");
        }
        String assignee = body == null || body.user() == null ? "" : body.user().trim();
        if (assignee.isEmpty()) {
            throw new ClaimReviewAssignRefusedException("Name the user to assign the review to.");
        }
        if (!claims.assignReview(claim, assignee)) {
            throw new ClaimReviewAssignRefusedException("Only a REVIEW claim's review can be assigned.");
        }
        responseOut.set(ClaimResponse.from(claim).withReviewUser(assignee));
    }
}
