package net.officefloor.hq.app.claim;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import net.officefloor.hq.app.rollup.Actor;
import net.officefloor.hq.app.sheet.InvalidSheetRequestException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

public class ValidateClaimDefer {
    public void validate(@RequestBody ClaimDeferRequest request, Out<ClaimDefer> body, Out<Actor> actor)
            throws InvalidSheetRequestException {
        String raw = request == null || request.until() == null ? "" : request.until().trim();
        try {
            actor.set(new Actor(request.user()));
            body.set(new ClaimDefer(LocalDate.parse(raw), request.user()));
        } catch (DateTimeParseException e) {
            throw new InvalidSheetRequestException("Not a date (yyyy-mm-dd): " + raw);
        }
    }
}
