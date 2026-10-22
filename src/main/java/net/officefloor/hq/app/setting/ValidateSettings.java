package net.officefloor.hq.app.setting;

import java.math.BigDecimal;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Bind the body and refuse it before anything is read or written: a signed-in user, and a clerk limit that is
 *  blank (no limit) or an amount 0.00 or more with up to two decimals. */
public class ValidateSettings {
    public void validate(@RequestBody SettingsRequest request, Out<SettingsInput> body) throws InvalidSettingException {
        if (request == null || request.user() == null || request.user().isBlank()) {
            throw new InvalidSettingException("Sign in before changing settings.");
        }
        String limit = request.clerkLimit() == null ? "" : request.clerkLimit().trim();
        if (!limit.isEmpty() && !limit.matches("\\d{1,12}(\\.\\d{1,2})?")) {
            throw new InvalidSettingException("The clerk limit must be an amount, 0.00 or more, with up to two decimals, or blank for no limit.");
        }
        body.set(new SettingsInput(request.user().trim(), limit.isEmpty() ? null : new BigDecimal(limit).setScale(2)));
    }
}
