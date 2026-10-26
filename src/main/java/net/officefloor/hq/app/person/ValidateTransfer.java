package net.officefloor.hq.app.person;

import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.session.ChangedBy;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Which sheet to transfer the person to. Publishes it, with the user making the change. */
public class ValidateTransfer {
    public void validate(@RequestBody TransferRequest request, Out<TransferInput> transfer, Out<ChangedBy> changedBy)
            throws BadRequestException {
        ChangedBy by = ChangedBy.of(request == null ? null : request.by());
        String to = request == null || request.to() == null ? "" : request.to().trim();
        if (to.isEmpty() || to.length() > 200) {
            throw new BadRequestException("Enter the sheet id to transfer to");
        }
        transfer.set(new TransferInput(to));
        changedBy.set(by);
    }
}
