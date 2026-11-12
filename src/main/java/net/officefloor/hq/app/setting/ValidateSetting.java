package net.officefloor.hq.app.setting;

import java.math.BigDecimal;
import java.util.regex.Pattern;
import net.officefloor.hq.app.sheet.InvalidSheetException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

public class ValidateSetting {
    /** 0.00 or more, with up to two decimals. */
    private static final Pattern LIMIT = Pattern.compile("\\d+(\\.\\d{1,2})?");

    public void validate(@RequestBody SettingRequest request, Out<SettingInput> body) throws InvalidSheetException {
        String key = request == null || request.key() == null ? "" : request.key().trim();
        if (!SettingStore.CLERK_LIMIT.equals(key)) {
            throw new InvalidSheetException("Not a setting: " + key);
        }
        String value = request.value() == null ? "" : request.value().trim();
        if (value.isEmpty()) {
            body.set(new SettingInput(key, null, request.user()));
            return;
        }
        if (!LIMIT.matcher(value).matches()) {
            throw new InvalidSheetException("The clerk limit must be 0.00 or more with up to two decimals, or blank");
        }
        body.set(new SettingInput(key, new BigDecimal(value).setScale(2).toPlainString(), request.user()));
    }
}
