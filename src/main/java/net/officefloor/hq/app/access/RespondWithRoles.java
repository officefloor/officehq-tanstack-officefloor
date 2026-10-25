package net.officefloor.hq.app.access;

import java.util.List;
import net.officefloor.hq.app.sheet.SheetResponse;
import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

public class RespondWithRoles {
    public void respond(@Val SheetResponse sheet, RoleRepository repository,
            ObjectResponse<List<RoleResponse>> response) {
        response.send(repository.list(sheet.id()).stream().map(RoleResponse::from).toList());
    }
}
