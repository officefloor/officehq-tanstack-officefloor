package net.officefloor.hq.app.setting;

import java.util.List;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;

public class ListRoles {
    public void list(@PathVariable("sheetId") String sheetId, RoleRepository roles,
            ObjectResponse<List<Role>> response) {
        response.send(roles.list(sheetId.trim()));
    }
}
