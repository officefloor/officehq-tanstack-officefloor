package net.officefloor.hq.app.setting;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.regex.Pattern;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

public class ValidateSettings {

    private static final Pattern AMOUNT = Pattern.compile("\\d{1,9}(\\.\\d{1,2})?");

    // Both are kept with two decimals. A blank floor leaves the floor as it is; a blank clerk limit is no
    // limit, unless a floor is saved with it, when the clerk limit is left as it is (and null leaves it
    // as it is too). Anything else that is not an amount of 0.00 or more with up to two decimals is refused.
    public void validate(@PathVariable("sheetId") String sheetId, @RequestBody SettingsRequest body,
            Out<SheetSettings> settingsOut) throws InvalidSettingException {
        String floor = body == null || body.floor() == null ? "" : body.floor().trim();
        if (!floor.isEmpty() && !AMOUNT.matcher(floor).matches()) {
            throw new InvalidSettingException("The floor must be an amount of 0.00 or more, with up to two decimals.");
        }
        String typed = body == null || body.clerkLimit() == null ? null : body.clerkLimit().trim();
        if (typed != null && !typed.isEmpty() && !AMOUNT.matcher(typed).matches()) {
            throw new InvalidSettingException("The clerk limit must be an amount of 0.00 or more, with up to two decimals.");
        }
        if (typed == null && floor.isEmpty()) {
            typed = "";
        }
        String limit = typed == null || (typed.isEmpty() && !floor.isEmpty()) ? null
                : typed.isEmpty() ? "" : twoDecimals(typed);
        settingsOut.set(new SheetSettings(sheetId.trim(), limit, floor.isEmpty() ? null : twoDecimals(floor)));
    }

    private static String twoDecimals(String amount) {
        return new BigDecimal(amount).setScale(2, RoundingMode.UNNECESSARY).toPlainString();
    }
}
