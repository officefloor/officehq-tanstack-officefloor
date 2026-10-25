package net.officefloor.hq.app.access;

import java.math.BigDecimal;
import java.util.regex.Pattern;
import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.session.ChangedBy;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** A setting's fields: a known key and its value. The clerk limit is an amount with up to two decimals, or blank for no limit. */
public class ValidateSetting {

    private static final Pattern AMOUNT = Pattern.compile("\\d{1,10}(\\.\\d{1,2})?");

    public void validate(@RequestBody SettingRequest request, Out<SettingInput> setting, Out<ChangedBy> changedBy)
            throws BadRequestException {
        ChangedBy by = ChangedBy.of(request == null ? null : request.by());
        String key = request == null || request.key() == null ? "" : request.key().trim();
        String text = request == null || request.value() == null ? "" : request.value().trim();
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
