package net.officefloor.hq.app.person;

import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Reads the body: undoing a merge names the signed-in user. */
public class ValidateMergeUndo {
    public void validate(@RequestBody MergeUndoRequest request, Out<MergeUndoRequest> body) throws InvalidPersonException {
        String by = request == null || request.by() == null ? "" : request.by().trim();
        if (by.isEmpty()) {
            throw new InvalidPersonException("The user undoing the merge is required");
        }
        body.set(new MergeUndoRequest(by));
    }
}
