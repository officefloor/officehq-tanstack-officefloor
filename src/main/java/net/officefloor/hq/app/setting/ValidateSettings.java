package net.officefloor.hq.app.setting;

import java.math.BigDecimal;
import java.util.regex.Pattern;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Reads the settings from the body, refusing a clerk limit or floor that is not empty or 0.00 or more with up to two decimals. */
public class ValidateSettings {

    private static final Pattern LIMIT = Pattern.compile("\\d{1,12}(\\.\\d{1,2})?");

    public void validate(@RequestBody SettingsRequest request, Out<SettingsInput> settingsOut)
            throws InvalidSettingException {
        String limit = request == null || request.clerkLimit() == null ? "" : request.clerkLimit().trim();
        if (!limit.isEmpty() && !LIMIT.matcher(limit).matches()) {
            throw new InvalidSettingException("The clerk limit must be empty or 0.00 or more, with up to two decimals.");
        }
        String floor = request == null || request.floor() == null ? "" : request.floor().trim();
        if (!floor.isEmpty() && !LIMIT.matcher(floor).matches()) {
            throw new InvalidSettingException("The floor must be 0.00 or more, with up to two decimals.");
        }
        settingsOut.set(new SettingsInput(limit.isEmpty() ? null : new BigDecimal(limit).setScale(2).toPlainString(),
                floor.isEmpty() ? null : new BigDecimal(floor).setScale(2).toPlainString()));
    }
}
