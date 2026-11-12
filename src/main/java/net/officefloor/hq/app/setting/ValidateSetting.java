package net.officefloor.hq.app.setting;

import java.math.BigDecimal;
import java.util.regex.Pattern;
import net.officefloor.hq.app.sheet.InvalidSheetException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

public class ValidateSetting {
    /** 0.00 or more, with up to two decimals. */
    private static final Pattern LIMIT = Pattern.compile("\\d+(\\.\\d{1,2})?");
    /** A whole number of days, 0 or more. */
    private static final Pattern DAYS = Pattern.compile("\\d{1,5}");

    public void validate(@RequestBody SettingRequest request, Out<SettingInput> body) throws InvalidSheetException {
        String key = request == null || request.key() == null ? "" : request.key().trim();
        boolean floor = SettingStore.FLOOR.equals(key);
        boolean terms = SettingStore.TERMS.equals(key);
        boolean late = SettingStore.LATE_PCT.equals(key);
        if (!SettingStore.CLERK_LIMIT.equals(key) && !floor && !terms && !late) {
            throw new InvalidSheetException("Not a setting: " + key);
        }
        String value = request.value() == null ? "" : request.value().trim();
        if (terms) {
            if (value.isEmpty()) {
                body.set(new SettingInput(key, String.valueOf(SettingStore.DEFAULT_TERMS), request.user()));
                return;
            }
            if (!DAYS.matcher(value).matches()) {
                throw new InvalidSheetException("The terms must be a whole number of days, 0 or more");
            }
            body.set(new SettingInput(key, String.valueOf(Integer.parseInt(value)), request.user()));
            return;
        }
        if (value.isEmpty() && floor) {
            body.set(new SettingInput(key, SettingStore.DEFAULT_FLOOR.toPlainString(), request.user()));
            return;
        }
        if (value.isEmpty() && late) {
            body.set(new SettingInput(key, SettingStore.DEFAULT_LATE_PCT.toPlainString(), request.user()));
            return;
        }
        if (value.isEmpty()) {
            body.set(new SettingInput(key, null, request.user()));
            return;
        }
        if (!LIMIT.matcher(value).matches()) {
            throw new InvalidSheetException(floor ? "The floor must be 0.00 or more with up to two decimals"
                    : late ? "The late percent must be 0.00 or more with up to two decimals"
                    : "The clerk limit must be 0.00 or more with up to two decimals, or blank");
        }
        body.set(new SettingInput(key, new BigDecimal(value).setScale(2).toPlainString(), request.user()));
    }
}
