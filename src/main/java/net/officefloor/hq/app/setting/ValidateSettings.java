package net.officefloor.hq.app.setting;

import java.math.BigDecimal;
import java.util.regex.Pattern;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

public class ValidateSettings {
    private static final Pattern LIMIT = Pattern.compile("\\d{1,15}(\\.\\d{1,2})?");

    public void validate(@RequestBody SettingsRequest request, Out<SettingsInput> body)
            throws InvalidSettingException {
        String limit = request == null || request.clerkLimit() == null ? "" : request.clerkLimit().trim();
        if (limit.isEmpty()) {
            body.set(new SettingsInput(null));
            return;
        }
        if (!LIMIT.matcher(limit).matches()) {
            throw new InvalidSettingException("A clerk limit is 0.00 or more, with up to two decimals");
        }
        body.set(new SettingsInput(new BigDecimal(limit).setScale(2)));
    }
}
