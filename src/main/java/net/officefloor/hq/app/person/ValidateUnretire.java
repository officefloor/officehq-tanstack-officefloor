package net.officefloor.hq.app.person;

import net.officefloor.hq.app.role.RoleStore;
import net.officefloor.hq.app.sheet.InvalidSheetException;
import net.officefloor.hq.app.sheet.SheetStore;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

public class ValidateUnretire {
    public void validate(@PathVariable("sheetId") String sheetId, @RequestBody UnretireRequest request,
            SheetStore sheets, Out<UnretireInput> body, RoleStore roles) throws InvalidSheetException {
        String user = request == null ? null : request.user();
        roles.refuseChange(sheetId.trim(), user);
        body.set(new UnretireInput(sheets.today(sheetId.trim()), user));
    }
}
