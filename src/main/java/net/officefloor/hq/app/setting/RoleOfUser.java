package net.officefloor.hq.app.setting;

import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/** The role the user acts in on the sheet on its today (a delegation covering it makes them a steward). */
public class RoleOfUser {
    public void role(@PathVariable("sheetId") String sheetId,
            @RequestParam(name = "user", required = false) String user, RoleRepository roles,
            ObjectResponse<Role> response) {
        String id = sheetId.trim();
        String name = user == null ? "" : user;
        response.send(new Role(id, name, roles.roleOf(id, name)));
    }
}
