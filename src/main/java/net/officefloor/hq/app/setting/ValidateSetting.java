package net.officefloor.hq.app.setting;

import java.math.BigDecimal;
import java.util.regex.Pattern;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

public class ValidateSetting {

    /** An amount: 0 or more, with at most two decimals. */
    private static final Pattern AMOUNT = Pattern.compile("\\d{1,10}(\\.\\d{1,2})?");

    public void validate(@RequestBody SettingRequest request, Out<ValidSetting> body) throws InvalidSettingException {
        String key = request.key() == null ? "" : request.key().trim();
        if (!"clerk-limit".equals(key)) {
            throw new InvalidSettingException("No such setting: " + key);
        }
        String raw = request.value() == null ? "" : request.value().trim();
        if (raw.isEmpty()) {
            body.set(new ValidSetting(key, null, request.user())); // blank: no limit
            return;
        }
        if (!AMOUNT.matcher(raw).matches()) {
            throw new InvalidSettingException("The clerk limit must be an amount of 0.00 or more with at most two decimals");
        }
        body.set(new ValidSetting(key, new BigDecimal(raw).setScale(2), request.user()));
    }
}
