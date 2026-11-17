package net.officefloor.hq.app.role;

import java.time.LocalDate;
import net.officefloor.hq.app.sheet.AsOf;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/** The role the user acts in on the sheet; read as of a date (see AsOf), the role they acted in then. */
public class RespondWithActingRole {
    public void respond(@PathVariable("sheetId") String sheetId, @PathVariable("user") String user,
            @RequestParam(value = "asOf", required = false) String asOf, RoleRepository roles,
            ObjectResponse<Role> response) {
        String id = sheetId.trim();
        String u = user == null ? "" : user.trim();
        LocalDate date = AsOf.parse(asOf);
        response.send(new Role(id, u, date == null ? roles.roleOf(id, u) : roles.roleOfAsOf(id, u, date)));
    }
}
