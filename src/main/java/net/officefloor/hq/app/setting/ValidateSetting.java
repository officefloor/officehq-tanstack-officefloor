package net.officefloor.hq.app.setting;

import java.math.BigDecimal;
import java.util.regex.Pattern;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

public class ValidateSetting {

    /** 0 or more, with up to two decimals. */
    private static final Pattern MONEY = Pattern.compile("\\d+(\\.\\d{1,2})?");

    public void validate(@PathVariable("sheetId") String sheetId, @PathVariable("key") String key,
            @RequestBody SettingRequest request, Out<Setting> setting) throws InvalidSettingException {
        String value = request == null || request.value() == null ? "" : request.value().strip();
        if (!ClerkLimit.KEY.equals(key) && !Floor.KEY.equals(key)) {
            throw new InvalidSettingException("No such setting: " + key);
        }
        if (Floor.KEY.equals(key)) {
            if (!MONEY.matcher(value).matches()) {
                throw new InvalidSettingException("The floor must be an amount with up to two decimals.");
            }
            value = new BigDecimal(value).setScale(2).toPlainString();
        } else if (!value.isEmpty()) {
            if (!MONEY.matcher(value).matches()) {
                throw new InvalidSettingException("The clerk limit must be blank or an amount with up to two decimals.");
            }
            value = new BigDecimal(value).setScale(2).toPlainString();
        }
        setting.set(new Setting(sheetId, key, value));
    }
}
