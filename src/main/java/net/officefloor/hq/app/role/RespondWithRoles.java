package net.officefloor.hq.app.role;

import java.util.List;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

public class RespondWithRoles {
    public void respond(@Val Sheet sheet, RoleRepository repository, ObjectResponse<List<RoleResponse>> response) {
        response.send(repository.findBySheetIdOrderByUserNameAsc(sheet.getId()).stream().map(RoleResponse::from).toList());
    }
}
