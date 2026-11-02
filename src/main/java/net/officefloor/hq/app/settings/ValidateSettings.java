package net.officefloor.hq.app.settings;

import java.math.BigDecimal;
import java.util.regex.Pattern;
import net.officefloor.hq.app.BadRequestException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Bind the settings form: the clerk limit is 0.00 or more with up to two decimals, blank meaning no
 *  limit; the floor is 0.00 or more with up to two decimals; the terms are a whole number of days, 0 to
 *  3650; the late percent is 0.00 or more with up to two decimals. A setting left out is not changed. */
public class ValidateSettings {
    private static final Pattern MONEY = Pattern.compile("\\d{1,9}(\\.\\d{1,2})?|\\.\\d{1,2}");
    private static final Pattern TERMS = Pattern.compile("\\d{1,4}");

    public void validate(@RequestBody SettingsRequest request, Out<SettingsInput> settings)
            throws BadRequestException {
        String limit = request == null || request.clerkLimit() == null ? null : request.clerkLimit().trim();
        String floor = request == null || request.floor() == null ? null : request.floor().trim();
        String terms = request == null || request.terms() == null ? null : request.terms().trim();
        if (limit != null && !limit.isEmpty() && !MONEY.matcher(limit).matches()) {
            throw new BadRequestException("clerk limit must be 0.00 or more with up to two decimals");
        }
        if (floor != null && !MONEY.matcher(floor).matches()) {
            throw new BadRequestException("floor must be 0.00 or more with up to two decimals");
        }
        String latePct = request == null || request.latePct() == null ? null : request.latePct().trim();
        if (terms != null && (!TERMS.matcher(terms).matches() || Integer.parseInt(terms) > 3650)) {
            throw new BadRequestException("terms must be a whole number of days from 0 to 3650");
        }
        if (latePct != null && !MONEY.matcher(latePct).matches()) {
            throw new BadRequestException("late percent must be 0.00 or more with up to two decimals");
        }
        settings.set(new SettingsInput(limit != null,
                limit == null || limit.isEmpty() ? null : new BigDecimal(limit).setScale(2),
                floor != null, floor == null ? null : new BigDecimal(floor).setScale(2),
                terms != null, terms == null ? null : Integer.parseInt(terms),
                latePct != null, latePct == null ? null : new BigDecimal(latePct).setScale(2)));
    }
}
