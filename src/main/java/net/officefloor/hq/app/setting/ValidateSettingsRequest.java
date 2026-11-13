package net.officefloor.hq.app.setting;

import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.util.regex.Pattern;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

public class ValidateSettingsRequest {

    /** 0.00 or more, with at most two decimals. */
    private static final Pattern AMOUNT = Pattern.compile("\\d{1,13}(\\.\\d{1,2})?|\\.\\d{1,2}");

    public void validate(@RequestBody @Valid SettingsRequest request, Out<ClerkLimit> limit, Out<Floor> floor)
            throws InvalidSettingException {
        limit.set(clerkLimit(request.clerkLimit()));
        floor.set(floor(request.floor()));
    }

    private static ClerkLimit clerkLimit(String requested) throws InvalidSettingException {
        if (requested == null) {
            return new ClerkLimit(false, null);
        }
        String value = requested.trim();
        if (value.isEmpty()) {
            return new ClerkLimit(true, null);
        }
        if (!AMOUNT.matcher(value).matches()) {
            throw new InvalidSettingException("The clerk limit must be 0.00 or more, with at most two decimals, or blank.");
        }
        return new ClerkLimit(true, new BigDecimal(value).setScale(2));
    }

    private static Floor floor(String requested) throws InvalidSettingException {
        if (requested == null) {
            return new Floor(null);
        }
        String value = requested.trim();
        if (value.isEmpty()) {
            return new Floor(SettingRepository.DEFAULT_FLOOR);
        }
        if (!AMOUNT.matcher(value).matches()) {
            throw new InvalidSettingException("The floor must be 0.00 or more, with at most two decimals, or blank.");
        }
        return new Floor(new BigDecimal(value).setScale(2));
    }
}
