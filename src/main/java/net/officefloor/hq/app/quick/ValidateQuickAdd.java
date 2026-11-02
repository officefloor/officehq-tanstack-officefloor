package net.officefloor.hq.app.quick;

import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.claim.ClaimInput;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Bind the quick-add bar: a name is required (trimmed), the amount follows the claim form's rule. */
public class ValidateQuickAdd {
    public void validate(@RequestBody QuickAddRequest request, Out<QuickAddInput> quick) throws BadRequestException {
        String name = request == null || request.name() == null ? "" : request.name().trim();
        String amount = request == null || request.amount() == null ? "" : request.amount().trim();
        if (name.isEmpty()) {
            throw new BadRequestException("name is required");
        }
        quick.set(new QuickAddInput(name, ClaimInput.amount(amount)));
    }
}
