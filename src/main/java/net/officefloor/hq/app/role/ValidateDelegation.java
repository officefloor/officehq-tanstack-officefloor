package net.officefloor.hq.app.role;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Reads the delegate form from the body; refuses a blank user name and a date that is not one. */
public class ValidateDelegation {

    public void validate(@RequestBody DelegationRequest request, Out<DelegationInput> body)
            throws InvalidDelegationException {
        String user = request == null || request.user() == null ? "" : request.user().trim();
        if (user.isEmpty()) {
            throw new InvalidDelegationException("The user name must not be blank.");
        }
        body.set(new DelegationInput(user, date(request.from()), date(request.to())));
    }

    private static LocalDate date(String value) throws InvalidDelegationException {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(value.trim());
        } catch (DateTimeParseException e) {
            throw new InvalidDelegationException("A date must be YYYY-MM-DD.");
        }
    }
}
