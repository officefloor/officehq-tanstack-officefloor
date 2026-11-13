package net.officefloor.hq.app.setting;

import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.util.regex.Pattern;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

public class ValidateSettingsRequest {

    /** 0.00 or more, with at most two decimals. */
    private static final Pattern AMOUNT = Pattern.compile("\\d{1,13}(\\.\\d{1,2})?|\\.\\d{1,2}");

    public void validate(@RequestBody @Valid SettingsRequest request, Out<ClerkLimit> limit)
            throws InvalidSettingException {
        String value = request.clerkLimit() == null ? "" : request.clerkLimit().trim();
        if (value.isEmpty()) {
            limit.set(new ClerkLimit(null));
            return;
        }
        if (!AMOUNT.matcher(value).matches()) {
            throw new InvalidSettingException("The clerk limit must be 0.00 or more, with at most two decimals, or blank.");
        }
        limit.set(new ClerkLimit(new BigDecimal(value).setScale(2)));
    }
}
