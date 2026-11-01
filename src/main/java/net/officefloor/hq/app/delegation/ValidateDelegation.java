package net.officefloor.hq.app.delegation;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import net.officefloor.hq.app.role.Role;
import net.officefloor.hq.app.role.Roles;
import net.officefloor.hq.app.sheet.SheetClock;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Only a steward given the role (not one lent it) delegates; the user is required, a blank from-date is the sheet's
 * today, and the to-date is required and not before the from-date.
 */
public class ValidateDelegation {
    public void validate(@PathVariable("sheet") String sheet, @RequestParam("user") String user,
            @RequestBody DelegationRequest request, Roles roles, SheetClock clock, Out<Delegation> body)
            throws InvalidDelegationException {
        if (!Role.STEWARD.equals(roles.given(sheet, user))) {
            throw new InvalidDelegationException("Only a steward delegates the steward role");
        }
        String delegate = request == null || request.user() == null ? "" : request.user().trim();
        String from = request == null || request.from() == null ? "" : request.from().trim();
        String to = request == null || request.to() == null ? "" : request.to().trim();
        if (delegate.isEmpty()) {
            throw new InvalidDelegationException("A delegation needs a user name");
        }
        LocalDate start;
        LocalDate end;
        try {
            start = from.isEmpty() ? clock.today(sheet) : LocalDate.parse(from);
            end = LocalDate.parse(to);
        } catch (DateTimeParseException e) {
            throw new InvalidDelegationException("A delegation needs a from-date and a to-date");
        }
        if (end.isBefore(start)) {
            throw new InvalidDelegationException("The to-date cannot be before the from-date");
        }
        body.set(new Delegation(0, user, delegate, start, end));
    }
}
