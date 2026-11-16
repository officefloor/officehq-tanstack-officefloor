package net.officefloor.hq.app.role;

import java.util.List;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;

/** The roles given on the sheet, by user name. */
public class RespondWithRoles {
    public void respond(@PathVariable("sheetId") String sheetId, RoleRepository roles,
            ObjectResponse<List<Role>> response) {
        response.send(roles.list(sheetId.trim()));
    }
}
