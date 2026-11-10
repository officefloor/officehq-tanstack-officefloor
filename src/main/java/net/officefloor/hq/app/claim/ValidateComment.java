package net.officefloor.hq.app.claim;

import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** A comment needs some text; it is kept trimmed. */
public class ValidateComment {
    public void validate(@RequestBody CommentRequest request, Out<CommentRequest> body) throws InvalidClaimException {
        String text = request == null || request.text() == null ? "" : request.text().trim();
        if (text.isEmpty()) throw new InvalidClaimException("A comment needs some text");
        body.set(new CommentRequest(text));
    }
}
