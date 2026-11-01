package net.officefloor.hq.app.plan;

import java.math.BigDecimal;
import java.util.regex.Pattern;
import net.officefloor.hq.app.rollup.Actor;
import net.officefloor.hq.app.sheet.InvalidSheetRequestException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Refuses a plan's new amount that is not more than 0 with up to two decimals. */
public class ValidatePlanAmount {
    private static final Pattern AMOUNT = Pattern.compile("\\d+(\\.\\d{1,2})?");

    public void validate(@RequestBody PlanAmountRequest request, Out<PlanAmount> body, Out<Actor> actor)
            throws InvalidSheetRequestException {
        String amount = request == null || request.amount() == null ? "" : request.amount().trim();
        if (!AMOUNT.matcher(amount).matches() || new BigDecimal(amount).signum() <= 0) {
            throw new InvalidSheetRequestException("The amount must be more than 0 with up to two decimals");
        }
        body.set(new PlanAmount(new BigDecimal(amount).setScale(2)));
        actor.set(new Actor(request.user()));
    }
}
