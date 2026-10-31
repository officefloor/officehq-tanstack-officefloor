package net.officefloor.hq.app.settings;

import java.math.BigDecimal;
import java.util.regex.Pattern;
import net.officefloor.hq.app.sheet.InvalidSheetRequestException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

public class ValidateSettings {
    /** An amount: 0.00 or more, with up to two decimals. */
    private static final Pattern LIMIT = Pattern.compile("\\d{1,10}(\\.\\d{1,2})?");

    public void validate(@PathVariable("sheetId") String sheetId, @RequestBody SettingsRequest request,
            Out<SettingsChange> change) throws InvalidSheetRequestException {
        String raw = request == null || request.clerkLimit() == null ? "" : request.clerkLimit().trim();
        if (!raw.isEmpty() && !LIMIT.matcher(raw).matches()) {
            throw new InvalidSheetRequestException("The clerk limit must be an amount with up to two decimals, or blank");
        }
        change.set(new SettingsChange(sheetId, raw.isEmpty() ? null : new BigDecimal(raw).setScale(2),
                request == null ? null : request.user()));
    }
}
