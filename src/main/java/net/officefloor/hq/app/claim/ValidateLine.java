package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Bind the body and refuse it before anything is read: a signed-in user, a label that is not empty (at most 200
 *  characters), a quantity that is a whole number of at least 1 and a unit amount with up to two decimals. */
public class ValidateLine {
    public void validate(@RequestBody LineRequest request, Out<LineInput> body) throws InvalidClaimException {
        if (request == null || request.user() == null || request.user().isBlank()) {
            throw new InvalidClaimException("Sign in before changing claims.");
        }
        String label = request.label() == null ? "" : request.label().trim();
        if (label.isEmpty()) {
            throw new InvalidClaimException("A line needs a label.");
        }
        if (label.length() > 200) {
            throw new InvalidClaimException("A line's label is at most 200 characters.");
        }
        String qty = request.qty() == null ? "" : request.qty().trim();
        if (!qty.matches("\\d{1,6}") || Integer.parseInt(qty) < 1) {
            throw new InvalidClaimException("A line's quantity must be a whole number, at least 1.");
        }
        String unit = request.unit() == null ? "" : request.unit().trim();
        if (!unit.matches("\\d{1,12}(\\.\\d{1,2})?")) {
            throw new InvalidClaimException("A line's unit amount must be a number with up to two decimals.");
        }
        body.set(new LineInput(request.user().trim(), label, Integer.parseInt(qty), new BigDecimal(unit).setScale(2)));
    }
}
