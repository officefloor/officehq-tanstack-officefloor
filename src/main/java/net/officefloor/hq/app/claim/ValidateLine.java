package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.session.ChangedBy;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** The user adding the line, and the line: a label (required), a whole quantity of 1 or more and a unit amount. */
public class ValidateLine {
    public void validate(@RequestBody LineRequest request, Out<ChangedBy> changedBy, Out<ClaimLine> line)
            throws BadRequestException {
        changedBy.set(ChangedBy.of(request == null ? null : request.by()));
        String label = request == null || request.label() == null ? "" : request.label().trim();
        String qty = request == null || request.qty() == null ? "" : request.qty().trim();
        if (label.isEmpty()) {
            throw new BadRequestException("Enter the line's label");
        }
        if (label.length() > 200) {
            throw new BadRequestException("A line's label is at most 200 characters");
        }
        if (!qty.matches("\\d{1,6}") || Integer.parseInt(qty) < 1) {
            throw new BadRequestException("A line's quantity is a whole number, at least 1");
        }
        line.set(new ClaimLine(label, Integer.parseInt(qty), ClaimAmount.of(request.unit())));
    }
}
