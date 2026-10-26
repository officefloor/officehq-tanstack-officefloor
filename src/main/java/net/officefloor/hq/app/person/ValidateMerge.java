package net.officefloor.hq.app.person;

import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.session.ChangedBy;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Which person to merge into. Publishes it, with the user making the change. */
public class ValidateMerge {
    public void validate(@RequestBody MergeRequest request, Out<MergeInput> merge, Out<ChangedBy> changedBy)
            throws BadRequestException {
        ChangedBy by = ChangedBy.of(request == null ? null : request.by());
        if (request == null || request.into() == null) {
            throw new BadRequestException("Choose a person to merge into");
        }
        merge.set(new MergeInput(request.into()));
        changedBy.set(by);
    }
}
