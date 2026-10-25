package net.officefloor.hq.app.access;

import java.util.Set;
import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.session.ChangedBy;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** A role assignment's fields: a user name (no spaces) and a role, clerk, steward or viewer. */
public class ValidateRole {

    private static final Set<String> ROLES = Set.of(Role.CLERK, Role.STEWARD, Role.VIEWER);

    public void validate(@RequestBody RoleRequest request, Out<RoleInput> role, Out<ChangedBy> changedBy)
            throws BadRequestException {
        ChangedBy by = ChangedBy.of(request == null ? null : request.by());
        String user = request == null || request.user() == null ? "" : request.user().trim();
        String asked = request == null || request.role() == null ? "" : request.role().trim();
        if (user.isEmpty()) {
            throw new BadRequestException("Enter a user name");
        }
        if (user.length() > 200 || user.chars().anyMatch(Character::isWhitespace)) {
            throw new BadRequestException("A user name is one word");
        }
        if (!ROLES.contains(asked)) {
            throw new BadRequestException("A role is clerk, steward or viewer");
        }
        role.set(new RoleInput(user, asked));
        changedBy.set(by);
    }
}
