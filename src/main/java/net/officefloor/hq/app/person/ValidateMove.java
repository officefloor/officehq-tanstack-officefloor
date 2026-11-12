package net.officefloor.hq.app.person;

import net.officefloor.hq.app.role.RoleStore;
import net.officefloor.hq.app.sheet.InvalidSheetException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

public class ValidateMove {
    public void validate(@PathVariable("sheetId") String sheetId, @RequestBody MoveRequest request, Out<MoveInput> body,
            RoleStore roles) throws InvalidSheetException {
        roles.refuseChange(sheetId.trim(), request == null ? null : request.user());
        String direction = request == null || request.direction() == null ? "" : request.direction().trim();
        if (!direction.equals("up") && !direction.equals("down")) {
            throw new InvalidSheetException("Move a person up or down");
        }
        body.set(new MoveInput(direction.equals("up"), request.user()));
    }
}
