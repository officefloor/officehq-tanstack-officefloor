package net.officefloor.hq.app.access;

import java.math.BigDecimal;
import java.util.regex.Pattern;
import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.session.ChangedBy;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** A setting's fields: a known key and its value. The terms are a whole number of days. The floor is an amount with up to two decimals. The clerk limit is an amount with up to two decimals, or blank for no limit. */
public class ValidateSetting {

    private static final Pattern AMOUNT = Pattern.compile("\\d{1,10}(\\.\\d{1,2})?");
    private static final Pattern DAYS = Pattern.compile("\\d{1,5}");

    public void validate(@RequestBody SettingRequest request, Out<SettingInput> setting, Out<ChangedBy> changedBy)
            throws BadRequestException {
        ChangedBy by = ChangedBy.of(request == null ? null : request.by());
        String key = request == null || request.key() == null ? "" : request.key().trim();
        String text = request == null || request.value() == null ? "" : request.value().trim();
        if (Setting.TERMS.equals(key)) {
            if (!DAYS.matcher(text).matches()) {
                throw new BadRequestException("Terms are a whole number of days");
            }
            setting.set(new SettingInput(key, String.valueOf(Integer.parseInt(text))));
            changedBy.set(by);
            return;
        }
        if (Setting.FLOOR.equals(key)) {
            if (!AMOUNT.matcher(text).matches()) {
                throw new BadRequestException("A floor is an amount with up to two decimals");
            }
            setting.set(new SettingInput(key, new BigDecimal(text).setScale(2).toPlainString()));
            changedBy.set(by);
            return;
        }
        if (!Setting.CLERK_LIMIT.equals(key)) {
            throw new BadRequestException("No such setting: " + key);
        }
        String value = null;
        if (!text.isEmpty()) {
            if (!AMOUNT.matcher(text).matches()) {
                throw new BadRequestException("A clerk limit is an amount with up to two decimals, or blank for no limit");
            }
            value = new BigDecimal(text).setScale(2).toPlainString();
        }
        setting.set(new SettingInput(key, value));
        changedBy.set(by);
    }
}
