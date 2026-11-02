package net.officefloor.hq.app.settings;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import net.officefloor.hq.app.BadRequestException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Bind the delegate form: a user name (no spaces), a from-date (blank: the sheet's today) and a to-date. */
public class ValidateDelegation {
    public void validate(@RequestBody DelegationRequest request, Out<DelegationInput> delegation) throws BadRequestException {
        String user = request == null || request.user() == null ? "" : request.user().trim();
        if (user.isEmpty()) {
            throw new BadRequestException("user is required");
        }
        if (user.chars().anyMatch(Character::isWhitespace)) {
            throw new BadRequestException("user must not contain spaces");
        }
        LocalDate from = date(request.from(), "from");
        LocalDate to = date(request.to(), "to");
        if (to == null) {
            throw new BadRequestException("to is required");
        }
        delegation.set(new DelegationInput(user, from, to));
    }

    private static LocalDate date(String value, String field) throws BadRequestException {
        String raw = value == null ? "" : value.trim();
        if (raw.isEmpty()) return null;
        try {
            return LocalDate.parse(raw);
        } catch (DateTimeParseException e) {
            throw new BadRequestException(field + " must be yyyy-mm-dd");
        }
    }
}
