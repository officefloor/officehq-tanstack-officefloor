package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.util.regex.Pattern;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Reads the body: the label (trimmed; a blank one is refused), the quantity (a whole number, at least 1), the unit
 *  amount (0.00 or more with up to two decimals) and who adds the line (a blank name is refused). */
public class ValidateLine {
    private static final Pattern QTY = Pattern.compile("\\d{1,6}");
    private static final Pattern UNIT = Pattern.compile("\\d{1,13}(\\.\\d{1,2})?");

    public void validate(@RequestBody LineRequest request, Out<ClaimLine> line) throws InvalidClaimException {
        String by = request == null || request.by() == null ? "" : request.by().trim();
        String label = request == null || request.label() == null ? "" : request.label().trim();
        String qty = request == null || request.qty() == null ? "" : request.qty().trim();
        String unit = request == null || request.unit() == null ? "" : request.unit().trim();
        if (by.isEmpty()) {
            throw new InvalidClaimException("Who adds the line is required");
        }
        if (label.isEmpty()) {
            throw new InvalidClaimException("A line needs a label");
        }
        if (!QTY.matcher(qty).matches() || Integer.parseInt(qty) < 1) {
            throw new InvalidClaimException("A line's quantity is a whole number, at least 1");
        }
        if (!UNIT.matcher(unit).matches()) {
            throw new InvalidClaimException("A line's unit amount is 0.00 or more with up to two decimals");
        }
        line.set(new ClaimLine(by, null, label, Integer.parseInt(qty), new BigDecimal(unit).setScale(2)));
    }
}
