package net.officefloor.hq.app.setting;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.regex.Pattern;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

public class ValidateSetting {
    private static final Pattern LIMIT = Pattern.compile("\\d{1,7}(\\.\\d{1,2})?");

    public void validate(@RequestBody SettingRequest request, Out<SettingInput> body) throws InvalidSettingException {
        String key = request == null || request.key() == null ? "" : request.key().trim();
        String value = request == null || request.value() == null ? "" : request.value().trim();
        if (!Settings.CLERK_LIMIT.equals(key)) {
            throw new InvalidSettingException("No such setting: " + key);
        }
        if (!value.isEmpty() && !LIMIT.matcher(value).matches()) {
            throw new InvalidSettingException("The clerk limit must be 0.00 or more with up to two decimals, or blank");
        }
        body.set(new SettingInput(key,
                value.isEmpty() ? "" : new BigDecimal(value).setScale(2, RoundingMode.UNNECESSARY).toPlainString()));
    }
}
