package net.officefloor.hq.app.role;

import java.util.List;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;

/** Responds with every role given on the sheet. */
public class RespondWithRoles {
    public void respond(@PathVariable("sheet") String sheet, Roles roles, ObjectResponse<List<Role>> response) {
        response.send(roles.list(sheet));
    }
}
