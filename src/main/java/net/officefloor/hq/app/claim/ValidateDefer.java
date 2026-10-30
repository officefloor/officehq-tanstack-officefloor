package net.officefloor.hq.app.claim;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

public class ValidateDefer {
    public void validate(@RequestBody DeferRequest request, Out<ValidDefer> body) throws InvalidClaimException {
        String until = request.until() == null ? "" : request.until().trim();
        try {
            body.set(new ValidDefer(request.user(), LocalDate.parse(until)));
        } catch (DateTimeParseException e) {
            throw new InvalidClaimException("A deferral needs a date to defer the claim until");
        }
    }
}
