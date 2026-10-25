package net.officefloor.hq.app.voidreason;

import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.session.ChangedBy;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** A void reason's text (trimmed, required). */
public class ValidateVoidReason {

    public void validate(@RequestBody VoidReasonRequest request, Out<VoidReasonInput> reason, Out<ChangedBy> changedBy)
            throws BadRequestException {
        ChangedBy by = ChangedBy.of(request == null ? null : request.by());
        String text = request == null || request.reason() == null ? "" : request.reason().trim();
        if (text.isEmpty()) {
            throw new BadRequestException("Enter a void reason");
        }
        reason.set(new VoidReasonInput(text));
        changedBy.set(by);
    }
}
