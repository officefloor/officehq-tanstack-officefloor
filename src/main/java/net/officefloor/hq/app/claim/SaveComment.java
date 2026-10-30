package net.officefloor.hq.app.claim;

import net.officefloor.plugin.variable.Val;

/** Keeps the comment on the claim, with its user and the sheet's today. */
public class SaveComment {
    public void save(@Val CommentRequest request, @Val Claim claim, Claims claims) {
        claims.comment(claim, request.user(), request.text().trim());
    }
}
