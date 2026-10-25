package net.officefloor.hq.app.rollover;

import java.util.regex.Pattern;
import net.officefloor.hq.app.AuditUser;
import net.officefloor.hq.app.role.Role;
import net.officefloor.hq.app.role.RoleRepository;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

/** Reads the rollover form from the body; refuses a year that is not four digits, and anyone acting as other than a
 *  steward. */
public class ValidateRollover {

    private static final Pattern YEAR = Pattern.compile("\\d{4}");

    public void validate(@RequestBody RolloverRequest request, @PathVariable("sheetId") String sheetId,
            @RequestHeader(name = "X-User", required = false) String user, RoleRepository roles,
            Out<RolloverRequest> body) throws InvalidRolloverException {
        String year = request == null || request.year() == null ? "" : request.year().trim();
        if (!YEAR.matcher(year).matches()) {
            throw new InvalidRolloverException("The fiscal year must be a year (yyyy).");
        }
        String role = roles.roleOf(sheetId, AuditUser.decode(user));
        if (!Role.STEWARD.equals(role)) {
            throw new InvalidRolloverException("A " + role + " cannot roll the year over.");
        }
        body.set(new RolloverRequest(year));
    }
}
