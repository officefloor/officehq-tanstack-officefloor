package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.session.ChangedBy;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** The amount to split off the claim. Publishes it with the user splitting it. */
public class ValidateSplit {
    public void validate(@RequestBody SplitRequest request, Out<SplitAmount> amount, Out<ChangedBy> changedBy)
            throws BadRequestException {
        ChangedBy by = ChangedBy.of(request == null ? null : request.by());
        amount.set(new SplitAmount(ClaimAmount.of(request == null ? null : request.amount())));
        changedBy.set(by);
    }
}
