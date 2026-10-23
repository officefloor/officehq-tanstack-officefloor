package net.officefloor.hq.app.role;

import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;

/** The role the user acts in on the sheet: steward while a delegation to them is in force, else the one assigned. */
public class RespondWithUserRole {
    public void respond(@PathVariable("sheetId") String sheetId, @PathVariable("user") String user,
            RoleRepository repository, ObjectResponse<RoleResponse> response) {
        String name = user.trim();
        response.send(new RoleResponse(name, repository.of(sheetId.trim(), name)));
    }
}
