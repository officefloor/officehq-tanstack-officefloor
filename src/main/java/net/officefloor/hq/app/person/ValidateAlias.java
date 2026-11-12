package net.officefloor.hq.app.person;

import net.officefloor.hq.app.role.RoleStore;
import net.officefloor.hq.app.sheet.InvalidSheetException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

/** Refuses a blank alias; publishes it without the spaces around it. */
public class ValidateAlias {
    public void validate(@PathVariable("sheetId") String sheetId, @RequestBody AliasRequest request,
            Out<AliasRequest> body, RoleStore roles) throws InvalidSheetException {
        roles.refuseChange(sheetId.trim(), request == null ? null : request.user());
        String alias = request == null || request.alias() == null ? "" : request.alias().trim();
        if (alias.isEmpty()) {
            throw new InvalidSheetException("An alias is required");
        }
        body.set(new AliasRequest(alias, request.user()));
    }
}
