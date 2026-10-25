package net.officefloor.hq.app.access;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.session.ChangedBy;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** A delegation's fields: a user name (no spaces), a from-date (yyyy-mm-dd; blank for the sheet's today) and a to-date (required). */
public class ValidateDelegate {

    public void validate(@RequestBody DelegateRequest request, Out<DelegateInput> delegate, Out<ChangedBy> changedBy)
            throws BadRequestException {
        ChangedBy by = ChangedBy.of(request == null ? null : request.by());
        String user = request.user() == null ? "" : request.user().trim();
        String from = request.from() == null ? "" : request.from().trim();
        String to = request.to() == null ? "" : request.to().trim();
        if (user.isEmpty()) {
            throw new BadRequestException("Enter a user name");
        }
        if (user.length() > 200 || user.chars().anyMatch(Character::isWhitespace)) {
            throw new BadRequestException("A user name is one word");
        }
        if (to.isEmpty()) {
            throw new BadRequestException("Enter a to-date");
        }
        delegate.set(new DelegateInput(user, from.isEmpty() ? null : date(from), date(to)));
        changedBy.set(by);
    }

    private static LocalDate date(String text) throws BadRequestException {
        try {
            return LocalDate.parse(text);
        } catch (DateTimeParseException e) {
            throw new BadRequestException("Enter a date as YYYY-MM-DD");
        }
    }
}
