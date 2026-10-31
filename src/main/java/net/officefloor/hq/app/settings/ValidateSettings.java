package net.officefloor.hq.app.settings;

import java.math.BigDecimal;
import java.util.regex.Pattern;
import net.officefloor.hq.app.rollup.Actor;
import net.officefloor.hq.app.sheet.InvalidSheetRequestException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

public class ValidateSettings {
    /** An amount: 0.00 or more, with up to two decimals. */
    private static final Pattern AMOUNT = Pattern.compile("\\d{1,10}(\\.\\d{1,2})?");

    public void validate(@PathVariable("sheetId") String sheetId, @RequestBody SettingsRequest request,
            Out<SettingsChange> change, Out<Actor> actor) throws InvalidSheetRequestException {
        String limit = request == null || request.clerkLimit() == null ? null : request.clerkLimit().trim();
        if (limit != null && !limit.isEmpty() && !AMOUNT.matcher(limit).matches()) {
            throw new InvalidSheetRequestException("The clerk limit must be an amount with up to two decimals, or blank");
        }
        String floor = request == null || request.floor() == null ? null : request.floor().trim();
        if (floor != null && !floor.isEmpty() && !AMOUNT.matcher(floor).matches()) {
            throw new InvalidSheetRequestException("The floor must be an amount with up to two decimals, or blank");
        }
        String by = request == null ? null : request.user();
        actor.set(new Actor(by));
        change.set(new SettingsChange(sheetId, limit != null,
                limit == null || limit.isEmpty() ? null : new BigDecimal(limit).setScale(2),
                floor == null ? null : floor.isEmpty() ? Settings.DEFAULT_FLOOR : new BigDecimal(floor).setScale(2),
                by));
    }
}
