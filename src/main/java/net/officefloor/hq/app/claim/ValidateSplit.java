package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.util.regex.Pattern;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PathVariable;
import net.officefloor.hq.app.setting.Settings;
import net.officefloor.hq.app.setting.ViewerRefusedException;

public class ValidateSplit {

    private static final Pattern AMOUNT = Pattern.compile("\\d+(\\.\\d{1,2})?");

    public void validate(@PathVariable("sheetId") String sheetId, @RequestBody SplitRequest request, Out<ValidSplit> body, Settings settings)
            throws InvalidClaimException, ViewerRefusedException {
        settings.refuseViewer(sheetId, request.user());
        String amount = request.amount() == null ? "" : request.amount().trim();
        if (!AMOUNT.matcher(amount).matches()) {
            throw new InvalidClaimException("The amount to split off must have at most two decimals");
        }
        BigDecimal value = new BigDecimal(amount).setScale(2);
        if (value.signum() <= 0) {
            throw new InvalidClaimException("The amount to split off must be more than 0.00");
        }
        body.set(new ValidSplit(request.user(), value));
    }
}
