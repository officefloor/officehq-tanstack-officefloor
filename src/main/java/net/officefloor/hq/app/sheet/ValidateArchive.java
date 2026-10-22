package net.officefloor.hq.app.sheet;

import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Bind the body and refuse it before anything is read: a signed-in user. */
public class ValidateArchive {
    public void validate(@RequestBody ArchiveRequest request, Out<ArchiveRequest> body) throws InvalidSheetException {
        if (request == null || request.user() == null || request.user().isBlank()) {
            throw new InvalidSheetException("Sign in before archiving the sheet.");
        }
        body.set(new ArchiveRequest(request.user().trim()));
    }
}
