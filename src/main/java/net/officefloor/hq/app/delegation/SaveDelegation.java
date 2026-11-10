package net.officefloor.hq.app.delegation;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.role.Role;
import net.officefloor.hq.app.role.RoleRepository;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/**
 * Only a steward by their own role may delegate (not one acting as a steward by a delegation). A blank
 * from-date is the sheet's today, a blank to-date the from-date; a to-date before the from-date is refused.
 */
public class SaveDelegation {
    public void save(@Val DelegationInput input, @Val Sheet sheet, RoleRepository roles,
            DelegationRepository repository, HttpServletRequest request, Out<Delegation> delegationOut)
            throws InvalidDelegationException {
        String by = Audit.user(request);
        if (!Role.STEWARD.equals(roles.assignedRoleOf(sheet.id(), by))) {
            throw new InvalidDelegationException("Only a steward may delegate the steward role");
        }
        LocalDate from = input.from().isEmpty() ? sheet.today() : date(input.from());
        LocalDate to = input.to().isEmpty() ? from : date(input.to());
        if (to.isBefore(from)) throw new InvalidDelegationException("A to-date is on or after the from-date");
        delegationOut.set(repository.save(new Delegation(0, sheet.id(), input.user(), by, from, to)));
    }

    private static LocalDate date(String value) throws InvalidDelegationException {
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException e) {
            throw new InvalidDelegationException("A date is yyyy-MM-dd");
        }
    }
}
