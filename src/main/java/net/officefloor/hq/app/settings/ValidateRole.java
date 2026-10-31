package net.officefloor.hq.app.settings;

import java.util.Locale;
import net.officefloor.hq.app.sheet.InvalidSheetRequestException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

public class ValidateRole {
    public void validate(@PathVariable("sheetId") String sheetId, @RequestBody RoleRequest request,
            Out<RoleChange> change) throws InvalidSheetRequestException {
        String name = request == null || request.name() == null ? "" : request.name().trim();
        if (name.isEmpty() || name.contains(" ")) {
            throw new InvalidSheetRequestException("A role is given to a user name");
        }
        String role = request.role() == null ? "" : request.role().trim();
        try {
            change.set(new RoleChange(sheetId, name, Role.valueOf(role.toUpperCase(Locale.ROOT)), request.user()));
        } catch (IllegalArgumentException e) {
            throw new InvalidSheetRequestException("Not a role (clerk or steward): " + role);
        }
    }
}
