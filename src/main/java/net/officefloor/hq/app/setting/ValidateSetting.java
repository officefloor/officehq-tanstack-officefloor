package net.officefloor.hq.app.setting;

import java.math.BigDecimal;
import java.util.regex.Pattern;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

/** Reads the body: the key is clerk-limit, its value blank (no limit) or an amount 0.00 or more with up to two
 *  decimals, kept with two. */
public class ValidateSetting {
    private static final Pattern AMOUNT = Pattern.compile("\\d{1,10}(\\.\\d{1,2})?");

    public void validate(@PathVariable("key") String key, @RequestBody SettingRequest request, Out<SettingValues> values)
            throws InvalidSettingException {
        String k = key == null ? "" : key.trim();
        if (!SettingRepository.CLERK_LIMIT.equals(k)) {
            throw new InvalidSettingException("Unknown setting " + k);
        }
        String value = request == null || request.value() == null ? "" : request.value().trim();
        if (!value.isEmpty() && !AMOUNT.matcher(value).matches()) {
            throw new InvalidSettingException("The clerk limit must be an amount with up to two decimals, or blank");
        }
        values.set(new SettingValues(k, value.isEmpty() ? null : new BigDecimal(value).setScale(2).toPlainString(),
                request.by() == null ? "" : request.by().trim()));
    }
}
