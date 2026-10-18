package net.officefloor.hq.app.setting;

import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;

/** The roles assigned on the sheet, by user name. */
public class RespondWithRoles {
    public void respond(@PathVariable("sheetId") String sheetId, UserRoleRepository repository,
            ObjectResponse<RolesResponse> response) {
        response.send(RolesResponse.from(repository.findBySheetIdOrderByUser(sheetId.trim())));
    }
}
