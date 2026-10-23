package net.officefloor.hq.app.rollover;

import net.officefloor.hq.app.period.InvalidPeriodException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Bind the body and refuse it before anything is read or written: a signed-in user. */
public class ValidateRolloverUndo {
    public void validate(@RequestBody RolloverUndoRequest request, Out<RolloverUndoInput> body)
            throws InvalidPeriodException {
        if (request == null || request.user() == null || request.user().isBlank()) {
            throw new InvalidPeriodException("Sign in before undoing a rollover.");
        }
        body.set(new RolloverUndoInput(request.user().trim()));
    }
}
