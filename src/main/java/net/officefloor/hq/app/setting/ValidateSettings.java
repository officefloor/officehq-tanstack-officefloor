package net.officefloor.hq.app.setting;

import java.math.BigDecimal;
import java.util.regex.Pattern;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Reads the settings form from the body; refuses a clerk limit that is negative or has more than two decimals.
 *  A blank clerk limit means no limit. */
public class ValidateSettings {

    private static final Pattern AMOUNT = Pattern.compile("\\d+(\\.\\d{1,2})?");

    public void validate(@RequestBody SettingsRequest request, Out<SettingsInput> body) throws InvalidSettingsException {
        String limit = request == null || request.clerkLimit() == null ? "" : request.clerkLimit().trim();
        if (limit.isEmpty()) {
            body.set(new SettingsInput(null));
            return;
        }
        if (!AMOUNT.matcher(limit).matches()) {
            throw new InvalidSettingsException("The clerk limit must be an amount, 0.00 or more, with up to two decimals, or blank.");
        }
        body.set(new SettingsInput(new BigDecimal(limit).setScale(2)));
    }
}
