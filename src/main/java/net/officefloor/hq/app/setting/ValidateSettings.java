package net.officefloor.hq.app.setting;

import java.math.BigDecimal;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Bind the body and refuse it before anything is read or written: a signed-in user, a clerk limit (when given)
 *  that is blank (no limit) or an amount 0.00 or more with up to two decimals, and a floor (when given) that is an
 *  amount 0.00 or more with up to two decimals, and terms (when given) that
 *  are a whole number of days. */
public class ValidateSettings {
    private static final String AMOUNT = "\\d{1,12}(\\.\\d{1,2})?";

    public void validate(@RequestBody SettingsRequest request, Out<SettingsInput> body) throws InvalidSettingException {
        if (request == null || request.user() == null || request.user().isBlank()) {
            throw new InvalidSettingException("Sign in before changing settings.");
        }
        String limit = request.clerkLimit() == null ? null : request.clerkLimit().trim();
        if (limit != null && !limit.isEmpty() && !limit.matches(AMOUNT)) {
            throw new InvalidSettingException("The clerk limit must be an amount, 0.00 or more, with up to two decimals, or blank for no limit.");
        }
        String floor = request.floor() == null ? null : request.floor().trim();
        if (floor != null && !floor.matches(AMOUNT)) {
            throw new InvalidSettingException("The floor must be an amount, 0.00 or more, with up to two decimals.");
        }
        String terms = request.terms() == null ? null : request.terms().trim();
        if (terms != null && !terms.matches("\\d{1,4}")) {
            throw new InvalidSettingException("The terms must be a whole number of days, 0 or more.");
        }
        body.set(new SettingsInput(request.user().trim(), limit != null,
                limit == null || limit.isEmpty() ? null : new BigDecimal(limit).setScale(2),
                floor == null ? null : new BigDecimal(floor).setScale(2),
                terms == null ? null : Integer.valueOf(terms)));
    }
}
