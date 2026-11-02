package net.officefloor.hq.app.settings;

import java.math.BigDecimal;
import java.util.regex.Pattern;
import net.officefloor.hq.app.BadRequestException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Bind the settings form: the clerk limit is 0.00 or more with up to two decimals, blank meaning no
 *  limit; the floor is 0.00 or more with up to two decimals. A setting left out is not changed. */
public class ValidateSettings {
    private static final Pattern MONEY = Pattern.compile("\\d{1,9}(\\.\\d{1,2})?|\\.\\d{1,2}");

    public void validate(@RequestBody SettingsRequest request, Out<SettingsInput> settings)
            throws BadRequestException {
        String limit = request == null || request.clerkLimit() == null ? null : request.clerkLimit().trim();
        String floor = request == null || request.floor() == null ? null : request.floor().trim();
        if (limit != null && !limit.isEmpty() && !MONEY.matcher(limit).matches()) {
            throw new BadRequestException("clerk limit must be 0.00 or more with up to two decimals");
        }
        if (floor != null && !MONEY.matcher(floor).matches()) {
            throw new BadRequestException("floor must be 0.00 or more with up to two decimals");
        }
        settings.set(new SettingsInput(limit != null,
                limit == null || limit.isEmpty() ? null : new BigDecimal(limit).setScale(2),
                floor != null, floor == null ? null : new BigDecimal(floor).setScale(2)));
    }
}
