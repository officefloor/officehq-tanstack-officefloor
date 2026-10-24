package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.util.regex.Pattern;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Reads the line from the body; refuses an empty label, a quantity that is not a whole number of at least 1, and a
 *  unit amount that is not a number with up to two decimals. */
public class ValidateLine {

    private static final Pattern QTY = Pattern.compile("\\d{1,9}");

    private static final Pattern UNIT = Pattern.compile("\\d{1,15}(\\.\\d{1,2})?");

    public void validate(@RequestBody LineRequest request, Out<LineInput> line) throws InvalidClaimException {
        String label = request == null || request.label() == null ? "" : request.label().trim();
        String qty = request == null || request.qty() == null ? "" : request.qty().trim();
        String unit = request == null || request.unit() == null ? "" : request.unit().trim();
        if (label.isEmpty()) {
            throw new InvalidClaimException("A line needs a label.");
        }
        if (!QTY.matcher(qty).matches() || Integer.parseInt(qty) < 1) {
            throw new InvalidClaimException("A line's quantity must be a whole number, at least 1.");
        }
        if (!UNIT.matcher(unit).matches()) {
            throw new InvalidClaimException("A line's unit amount must be a number with up to two decimals.");
        }
        line.set(new LineInput(label, Integer.parseInt(qty), new BigDecimal(unit).setScale(2)));
    }
}
