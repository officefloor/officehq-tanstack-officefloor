package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.util.regex.Pattern;
import net.officefloor.hq.app.rollup.Actor;
import net.officefloor.hq.app.sheet.InvalidSheetRequestException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

public class ValidateClaimLine {
    private static final Pattern QTY = Pattern.compile("\\d{1,9}");
    private static final Pattern UNIT = Pattern.compile("\\d+(\\.\\d{1,2})?");

    public void validate(@RequestBody ClaimLineRequest request, Out<ClaimLine> body, Out<Actor> actor)
            throws InvalidSheetRequestException {
        String user = request == null || request.user() == null ? "" : request.user().trim();
        if (user.isEmpty()) throw new InvalidSheetRequestException("Who adds the line must be named");
        String label = request.label() == null ? "" : request.label().trim();
        if (label.isEmpty()) throw new InvalidSheetRequestException("A line must have a label");
        String qty = request.qty() == null ? "" : request.qty().trim();
        if (!QTY.matcher(qty).matches() || Integer.parseInt(qty) < 1) {
            throw new InvalidSheetRequestException("A line's quantity must be a whole number, at least 1");
        }
        String unit = request.unit() == null ? "" : request.unit().trim();
        if (!UNIT.matcher(unit).matches()) {
            throw new InvalidSheetRequestException("A line's unit amount must be a number with up to two decimals");
        }
        actor.set(new Actor(user));
        body.set(new ClaimLine(label, Integer.parseInt(qty), new BigDecimal(unit).setScale(2), user));
    }
}
