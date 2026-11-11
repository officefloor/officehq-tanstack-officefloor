package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.util.regex.Pattern;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** A line needs a label, a whole quantity of at least 1 and a unit amount of up to two decimals. */
public class ValidateLine {
    private static final Pattern QTY = Pattern.compile("\\d{1,9}");
    private static final Pattern UNIT = Pattern.compile("\\d{1,13}(\\.\\d{1,2})?");

    public void validate(@RequestBody LineRequest request, Out<ClaimLine> body) throws InvalidClaimException {
        String label = request == null || request.label() == null ? "" : request.label().trim();
        if (label.isEmpty()) throw new InvalidClaimException("A line needs a label");
        if (label.length() > 200) throw new InvalidClaimException("A label is at most 200 characters");
        String qty = request.qty() == null ? "" : request.qty().trim();
        if (!QTY.matcher(qty).matches() || Integer.parseInt(qty) < 1) {
            throw new InvalidClaimException("A quantity is a whole number of at least 1");
        }
        String unit = request.unit() == null ? "" : request.unit().trim();
        if (!UNIT.matcher(unit).matches()) {
            throw new InvalidClaimException("A unit amount is a number with up to two decimals");
        }
        body.set(new ClaimLine(label, Integer.parseInt(qty), new BigDecimal(unit).setScale(2)));
    }
}
