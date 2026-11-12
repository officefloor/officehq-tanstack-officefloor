package net.officefloor.hq.app.plan;

import java.math.BigDecimal;
import java.util.regex.Pattern;
import net.officefloor.hq.app.role.RoleStore;
import net.officefloor.hq.app.sheet.InvalidSheetException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

/** Refuses a change of a plan's amount by a viewer, on an archived sheet, or with an amount a plan would refuse
 *  (one that is not a number with up to two decimals). Publishes the amount with two decimals. */
public class ValidatePlanAmount {
    private static final Pattern AMOUNT = Pattern.compile("\\d+(\\.\\d{1,2})?");

    public void validate(@PathVariable("sheetId") String sheetId, @RequestBody PlanAmountRequest request,
            RoleStore roles, Out<PlanAmountRequest> body) throws InvalidSheetException {
        String user = request == null || request.user() == null ? "" : request.user().trim();
        roles.refuseChange(sheetId.trim(), user);
        String amount = request == null || request.amount() == null ? "" : request.amount().trim();
        if (!AMOUNT.matcher(amount).matches()) {
            throw new InvalidSheetException("The amount must be a number with up to two decimals");
        }
        body.set(new PlanAmountRequest(new BigDecimal(amount).setScale(2).toPlainString(), user));
    }
}
