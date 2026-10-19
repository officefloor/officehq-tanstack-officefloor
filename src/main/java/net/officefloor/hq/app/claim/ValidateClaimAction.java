package net.officefloor.hq.app.claim;

import java.util.Locale;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;

public class ValidateClaimAction {
    public void validate(@PathVariable("action") String action, Out<ClaimAction> actionOut)
            throws InvalidClaimException {
        try {
            actionOut.set(ClaimAction.valueOf(action.toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException e) {
            throw new InvalidClaimException("No such claim action: " + action);
        }
    }
}
