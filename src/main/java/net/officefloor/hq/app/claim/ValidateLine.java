package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.util.regex.Pattern;
import net.officefloor.hq.app.role.RoleStore;
import net.officefloor.hq.app.sheet.InvalidSheetException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

public class ValidateLine {
    /** A whole quantity. */
    private static final Pattern QTY = Pattern.compile("\\d{1,9}");
    /** A unit amount with up to two decimals. */
    private static final Pattern UNIT = Pattern.compile("\\d+(\\.\\d{1,2})?");

    public void validate(@PathVariable("sheetId") String sheetId, @RequestBody LineRequest request, Out<LineInput> body, RoleStore roles) throws InvalidSheetException {
        roles.refuseChange(sheetId.trim(), request == null ? null : request.user());
        String label = request.label() == null ? "" : request.label().trim();
        if (label.isEmpty()) {
            throw new InvalidSheetException("A line needs a label");
        }
        String qty = request.qty() == null ? "" : request.qty().trim();
        if (!QTY.matcher(qty).matches() || Integer.parseInt(qty) < 1) {
            throw new InvalidSheetException("The quantity must be a whole number, at least 1");
        }
        String unit = request.unit() == null ? "" : request.unit().trim();
        if (!UNIT.matcher(unit).matches()) {
            throw new InvalidSheetException("The unit amount must be a number with up to two decimals");
        }
        body.set(new LineInput(request.user() == null ? "" : request.user().trim(), label, Integer.parseInt(qty),
                new BigDecimal(unit).setScale(2)));
    }
}
