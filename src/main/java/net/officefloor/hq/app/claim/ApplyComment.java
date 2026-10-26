package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.sheet.Sheets;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import net.officefloor.server.http.ServerHttpConnection;
import org.springframework.web.bind.annotation.RequestBody;

public class ApplyComment {
    // A comment says something: an empty one is refused. It is kept with its writer and the sheet's today.
    public void apply(@RequestBody(required = false) CommentRequest body, @Val Claim claim, Claims claims, Sheets sheets,
            ServerHttpConnection connection, Out<ClaimComment> commentOut, Out<ClaimResponse> responseOut)
            throws ClaimCommentRefusedException {
        String text = body == null || body.text() == null ? "" : body.text().trim();
        if (text.isEmpty()) {
            throw new ClaimCommentRefusedException("A comment cannot be empty.");
        }
        ClaimComment comment = new ClaimComment(Audit.user(connection).trim(), sheets.today(claim.sheetId()), text);
        claims.comment(claim, comment.user(), comment.on(), comment.text());
        commentOut.set(comment);
        responseOut.set(ClaimResponse.from(claim)
                .withComments(claims.comments(claim.sheetId()).getOrDefault(claim.id(), java.util.List.of())));
    }
}
