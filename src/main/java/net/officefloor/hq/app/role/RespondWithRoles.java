package net.officefloor.hq.app.role;

import java.util.List;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;

public class RespondWithRoles {
    public void respond(@PathVariable("sheetId") String sheetId, RoleStore store,
            ObjectResponse<List<RoleResponse>> response) {
        response.send(store.list(sheetId.trim()));
    }
}
