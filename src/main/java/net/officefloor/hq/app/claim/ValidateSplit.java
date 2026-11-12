package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.util.regex.Pattern;
import net.officefloor.hq.app.role.RoleStore;
import net.officefloor.hq.app.sheet.InvalidSheetException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

public class ValidateSplit {
    /** A number with up to two decimals. */
    private static final Pattern AMOUNT = Pattern.compile("\\d+(\\.\\d{1,2})?");

    public void validate(@PathVariable("sheetId") String sheetId, @RequestBody SplitRequest request, Out<SplitInput> body,
            RoleStore roles) throws InvalidSheetException {
        roles.refuseViewer(sheetId.trim(), request == null ? null : request.user());
        String amount = request == null || request.amount() == null ? "" : request.amount().trim();
        if (!AMOUNT.matcher(amount).matches()) {
            throw new InvalidSheetException("The amount must be a number with up to two decimals");
        }
        BigDecimal value = new BigDecimal(amount).setScale(2);
        if (value.signum() <= 0) {
            throw new InvalidSheetException("The amount split off must be more than 0.00");
        }
        body.set(new SplitInput(value, request.user()));
    }
}
