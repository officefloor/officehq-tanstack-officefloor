package net.officefloor.hq.app.claim;

import net.officefloor.plugin.variable.Val;

public class SaveComment {
    public void save(@Val Claim claim, @Val ClaimComment comment, ClaimCommentRepository comments) {
        comments.add(claim, comment);
    }
}
