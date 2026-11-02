package net.officefloor.hq.app.role;

import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/** Responds with the role the user acts in on the sheet (steward while lent the role; under an as-of date, as then). */
public class RespondWithUserRole {
    public void respond(@PathVariable("sheet") String sheet, @RequestParam("user") String user, Roles roles,
            ObjectResponse<Role> response) {
        response.send(new Role(user, roles.of(sheet, user)));
    }
}
