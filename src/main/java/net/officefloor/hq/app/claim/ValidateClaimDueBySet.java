package net.officefloor.hq.app.claim;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import net.officefloor.hq.app.rollup.Actor;
import net.officefloor.hq.app.sheet.InvalidSheetRequestException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

public class ValidateClaimDueBySet {
    public void validate(@RequestBody ClaimDueBySetRequest request, Out<ClaimDueBySet> body, Out<Actor> actor)
            throws InvalidSheetRequestException {
        String raw = request == null || request.dueBy() == null ? "" : request.dueBy().trim();
        try {
            actor.set(new Actor(request.user()));
            body.set(new ClaimDueBySet(LocalDate.parse(raw), request.user()));
        } catch (DateTimeParseException e) {
            throw new InvalidSheetRequestException("Not a date (yyyy-mm-dd): " + raw);
        }
    }
}
