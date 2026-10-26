package net.officefloor.hq.app.setting;

import java.util.List;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;

public class RespondWithRoles {
    public void respond(@PathVariable("sheetId") String sheetId, Settings settings,
            ObjectResponse<List<RoleResponse>> response) {
        response.send(settings.roles(sheetId.trim()).stream().map(RoleResponse::from).toList());
    }
}
