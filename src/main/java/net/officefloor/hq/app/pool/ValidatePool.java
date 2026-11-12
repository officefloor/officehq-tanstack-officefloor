package net.officefloor.hq.app.pool;

import net.officefloor.hq.app.crew.CrewStore;
import net.officefloor.hq.app.role.RoleStore;
import net.officefloor.hq.app.sheet.InvalidSheetException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

/** Refuses a pool by a viewer, a blank name, or a name already a pool or a crew on the sheet. */
public class ValidatePool {
    public void validate(@PathVariable("sheetId") String sheetId, @RequestBody PoolRequest request,
            Out<PoolInput> body, RoleStore roles, PoolStore pools,
            CrewStore crews) throws InvalidSheetException {
        String sheet = sheetId.trim();
        String user = request == null || request.user() == null ? "" : request.user().trim();
        roles.refuseChange(sheet, user);
        String name = request == null || request.name() == null ? "" : request.name().trim();
        if (name.isEmpty()) {
            throw new InvalidSheetException("A pool name is required");
        }
        if (pools.has(sheet, name)) {
            throw new InvalidSheetException(name + " is already a pool");
        }
        if (crews.has(sheet, name)) {
            throw new InvalidSheetException(name + " is already a crew");
        }
        body.set(new PoolInput(name, user));
    }
}
