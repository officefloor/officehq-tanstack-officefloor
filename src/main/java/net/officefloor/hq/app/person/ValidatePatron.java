package net.officefloor.hq.app.person;

import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.session.ChangedBy;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Which person to name as patron. Publishes it, with the user making the change. */
public class ValidatePatron {
    public void validate(@RequestBody PatronRequest request, Out<PatronInput> patron, Out<ChangedBy> changedBy)
            throws BadRequestException {
        ChangedBy by = ChangedBy.of(request == null ? null : request.by());
        if (request == null || request.patron() == null) {
            throw new BadRequestException("Choose a patron");
        }
        patron.set(new PatronInput(request.patron()));
        changedBy.set(by);
    }
}
