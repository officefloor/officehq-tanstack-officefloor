package net.officefloor.hq.app.setting;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.regex.Pattern;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Reads the settings form from the body; refuses a clerk limit or floor that is negative or has more than two
 *  decimals, and terms that are not a whole number of days, and a late percent that is negative or has more than two decimals, and a policy date that is not a calendar date
 *  (YYYY-MM-DD). A blank clerk limit means no limit; a blank floor is
 *  1.00; blank terms are 14; a blank late percent is 0.00; a blank policy date is the sheet's today. A key left out is not changed. */
public class ValidateSettings {

    private static final Pattern DAYS = Pattern.compile("\\d{1,5}");

    private static final Pattern AMOUNT = Pattern.compile("\\d+(\\.\\d{1,2})?");

    public void validate(@RequestBody SettingsRequest request, Out<SettingsInput> body) throws InvalidSettingsException {
        String limit = request == null ? null : request.clerkLimit();
        String floor = request == null ? null : request.floor();
        BigDecimal limitValue = null;
        if (limit != null && !limit.trim().isEmpty()) {
            if (!AMOUNT.matcher(limit.trim()).matches()) {
                throw new InvalidSettingsException("The clerk limit must be an amount, 0.00 or more, with up to two decimals, or blank.");
            }
            limitValue = new BigDecimal(limit.trim()).setScale(2);
        }
        BigDecimal floorValue = Sheet.DEFAULT_FLOOR;
        if (floor != null && !floor.trim().isEmpty()) {
            if (!AMOUNT.matcher(floor.trim()).matches()) {
                throw new InvalidSettingsException("The floor must be an amount, 0.00 or more, with up to two decimals, or blank.");
            }
            floorValue = new BigDecimal(floor.trim()).setScale(2);
        }
        String terms = request == null ? null : request.terms();
        int termsValue = Sheet.DEFAULT_TERMS;
        if (terms != null && !terms.trim().isEmpty()) {
            if (!DAYS.matcher(terms.trim()).matches()) {
                throw new InvalidSettingsException("The terms must be a whole number of days, 0 or more, or blank.");
            }
            termsValue = Integer.parseInt(terms.trim());
        }
        String late = request == null ? null : request.latePct();
        BigDecimal lateValue = Sheet.DEFAULT_LATE_PCT;
        if (late != null && !late.trim().isEmpty()) {
            if (!AMOUNT.matcher(late.trim()).matches()) {
                throw new InvalidSettingsException("The late percent must be a percent, 0.00 or more, with up to two decimals, or blank.");
            }
            lateValue = new BigDecimal(late.trim()).setScale(2);
        }
        String policy = request == null ? null : request.policyDate();
        LocalDate policyValue = null;
        if (policy != null && !policy.trim().isEmpty()) {
            try {
                policyValue = LocalDate.parse(policy.trim());
            } catch (DateTimeParseException e) {
                throw new InvalidSettingsException("The policy date must be a date (YYYY-MM-DD), or blank.");
            }
        }
        body.set(new SettingsInput(limit != null, limitValue, floor != null, floorValue, terms != null, termsValue,
                late != null, lateValue, policy != null, policyValue));
    }
}
