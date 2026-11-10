package net.officefloor.hq.app.setting;

import java.math.BigDecimal;
import java.util.regex.Pattern;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

public class ValidateSettings {
    private static final Pattern FIGURE = Pattern.compile("\\d{1,15}(\\.\\d{1,2})?");
    private static final Pattern DAYS = Pattern.compile("\\d{1,5}");

    public void validate(@RequestBody SettingsRequest request, Out<SettingsInput> body)
            throws InvalidSettingException {
        boolean limitChanged = request != null && request.clerkLimit() != null;
        BigDecimal limit = null;
        if (limitChanged && !request.clerkLimit().trim().isEmpty()) {
            String value = request.clerkLimit().trim();
            if (!FIGURE.matcher(value).matches()) {
                throw new InvalidSettingException("A clerk limit is 0.00 or more, with up to two decimals");
            }
            limit = new BigDecimal(value).setScale(2);
        }
        BigDecimal floor = null;
        if (request != null && request.floor() != null) {
            String value = request.floor().trim();
            if (!FIGURE.matcher(value).matches()) {
                throw new InvalidSettingException("A floor is 0.00 or more, with up to two decimals");
            }
            floor = new BigDecimal(value).setScale(2);
        }
        Integer terms = null;
        if (request != null && request.terms() != null) {
            String value = request.terms().trim();
            if (!DAYS.matcher(value).matches()) {
                throw new InvalidSettingException("Terms are a whole number of days, 0 or more");
            }
            terms = Integer.valueOf(value);
        }
        body.set(new SettingsInput(limitChanged, limit, floor, terms));
    }
}
