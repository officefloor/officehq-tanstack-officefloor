package net.officefloor.hq.app.sheet;

import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.session.ChangedBy;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Which sheet to clone the sheet to. Publishes it, with the user making the change. */
public class ValidateClone {
    public void validate(@RequestBody CloneRequest request, Out<CloneInput> clone, Out<ChangedBy> changedBy)
            throws BadRequestException {
        ChangedBy by = ChangedBy.of(request == null ? null : request.by());
        String to = request == null || request.to() == null ? "" : request.to().trim();
        if (to.isEmpty() || to.length() > 200) {
            throw new BadRequestException("Enter the sheet id to clone to");
        }
        clone.set(new CloneInput(to));
        changedBy.set(by);
    }
}
