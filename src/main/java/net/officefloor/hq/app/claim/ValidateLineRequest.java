package net.officefloor.hq.app.claim;

import jakarta.validation.Valid;
import java.math.BigDecimal;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Reads the line's body once, publishing the line (its claim id not yet known: 0); refuses (400) a blank label, a
 * quantity that is not a whole number of at least 1, or a unit amount that is not 0.00 or more with at most two decimals.
 */
public class ValidateLineRequest {
    public void validate(@RequestBody @Valid LineRequest request, Out<ClaimLine> line) throws InvalidClaimException {
        String label = request == null || request.label() == null ? "" : request.label().trim();
        if (label.isEmpty()) {
            throw new InvalidClaimException("Type a label.");
        }
        String qty = request.qty() == null ? "" : request.qty().trim();
        if (!qty.matches("\\d{1,6}") || Integer.parseInt(qty) < 1) {
            throw new InvalidClaimException("Type a quantity: a whole number, at least 1.");
        }
        String unit = request.unit() == null ? "" : request.unit().trim();
        if (!unit.matches("\\d{1,12}(\\.\\d{1,2})?|\\.\\d{1,2}")) {
            throw new InvalidClaimException("Type a unit amount with at most two decimals.");
        }
        line.set(new ClaimLine(0, label, Integer.parseInt(qty), new BigDecimal(unit).setScale(2)));
    }
}
