package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import net.officefloor.hq.app.rollup.Actor;
import net.officefloor.hq.app.sheet.InvalidSheetRequestException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

public class ValidateClaimSplit {
    public void validate(@RequestBody ClaimSplitRequest request, Out<ClaimSplit> body, Out<Actor> actor)
            throws InvalidSheetRequestException {
        String text = request == null || request.amount() == null ? "" : request.amount().trim();
        if (!text.matches("\\d+(\\.\\d{1,2})?")) {
            throw new InvalidSheetRequestException("A split needs an amount with up to two decimals: " + text);
        }
        BigDecimal amount = new BigDecimal(text).setScale(2);
        if (amount.signum() <= 0) {
            throw new InvalidSheetRequestException("A split amount must be more than 0.00");
        }
        actor.set(new Actor(request.user()));
        body.set(new ClaimSplit(amount, request.user()));
    }
}
