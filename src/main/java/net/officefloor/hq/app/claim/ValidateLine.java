package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.util.regex.Pattern;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import net.officefloor.hq.app.setting.Settings;
import net.officefloor.hq.app.setting.ViewerRefusedException;

/** A line needs a label, a whole quantity of at least 1 and a unit amount with at most two decimals; a viewer, or any user
 *  on an archived sheet, is refused. */
public class ValidateLine {

    private static final Pattern QTY = Pattern.compile("\\d{1,9}");

    public void validate(@PathVariable("sheetId") String sheetId, @RequestBody LineRequest request, Out<ClaimLine> body,
            Out<LineRequest> requestOut, Settings settings) throws ViewerRefusedException, InvalidClaimException {
        settings.refuseViewer(sheetId, request.user());
        String label = request.label() == null ? "" : request.label().trim();
        if (label.isEmpty()) {
            throw new InvalidClaimException("A line needs a label");
        }
        String qty = request.qty() == null ? "" : request.qty().trim();
        if (!QTY.matcher(qty).matches() || Integer.parseInt(qty) < 1) {
            throw new InvalidClaimException("A line's quantity must be a whole number, at least 1");
        }
        BigDecimal unit = ValidateClaim.amount(request.unit());
        body.set(new ClaimLine(label, Integer.parseInt(qty), unit));
        requestOut.set(request);
    }
}
