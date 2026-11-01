package net.officefloor.hq.app.setting;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.regex.Pattern;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

public class ValidateSetting {
    private static final Pattern LIMIT = Pattern.compile("\\d{1,7}(\\.\\d{1,2})?");
    private static final Pattern DAYS = Pattern.compile("\\d{1,4}");

    public void validate(@RequestBody SettingRequest request, Out<SettingInput> body) throws InvalidSettingException {
        String key = request == null || request.key() == null ? "" : request.key().trim();
        String value = request == null || request.value() == null ? "" : request.value().trim();
        if (!Settings.CLERK_LIMIT.equals(key) && !Settings.FLOOR.equals(key) && !Settings.TERMS.equals(key)) {
            throw new InvalidSettingException("No such setting: " + key);
        }
        // The terms are a whole number of days; blank takes the default.
        if (Settings.TERMS.equals(key)) {
            if (!value.isEmpty() && !DAYS.matcher(value).matches()) {
                throw new InvalidSettingException("The terms must be a whole number of days, 0 or more, or blank");
            }
            body.set(new SettingInput(key, Integer.toString(Integer.parseInt(value.isEmpty() ? Settings.DEFAULT_TERMS : value))));
            return;
        }
        if (!value.isEmpty() && !LIMIT.matcher(value).matches()) {
            throw new InvalidSettingException("The " + key + " must be 0.00 or more with up to two decimals, or blank");
        }
        // A blank floor takes its default: the floor always has a value.
        if (Settings.FLOOR.equals(key) && value.isEmpty()) {
            value = Settings.DEFAULT_FLOOR;
        }
        body.set(new SettingInput(key,
                value.isEmpty() ? "" : new BigDecimal(value).setScale(2, RoundingMode.UNNECESSARY).toPlainString()));
    }
}
