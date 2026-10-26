package net.officefloor.hq.app.setting;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.regex.Pattern;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

public class ValidateSettings {

    private static final Pattern AMOUNT = Pattern.compile("\\d{1,9}(\\.\\d{1,2})?");

    // The clerk limit is kept with two decimals; blank is no limit, anything else that is not an
    // amount of 0.00 or more with up to two decimals is refused.
    public void validate(@PathVariable("sheetId") String sheetId, @RequestBody SettingsRequest body,
            Out<SheetSettings> settingsOut) throws InvalidSettingException {
        String typed = body == null || body.clerkLimit() == null ? "" : body.clerkLimit().trim();
        if (!typed.isEmpty() && !AMOUNT.matcher(typed).matches()) {
            throw new InvalidSettingException("The clerk limit must be an amount of 0.00 or more, with up to two decimals.");
        }
        String limit = typed.isEmpty() ? "" : new BigDecimal(typed).setScale(2, RoundingMode.UNNECESSARY).toPlainString();
        settingsOut.set(new SheetSettings(sheetId.trim(), limit));
    }
}
