package net.officefloor.hq.app.person;

import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PathVariable;
import net.officefloor.hq.app.setting.Settings;
import net.officefloor.hq.app.setting.ViewerRefusedException;

/** Refuses a blank alias; publishes the alias trimmed. */
public class ValidateAlias {
    public void validate(@PathVariable("sheetId") String sheetId, @RequestBody AliasRequest request, Out<AliasRequest> body, Settings settings)
            throws InvalidPersonException, ViewerRefusedException {
        settings.refuseViewer(sheetId, request.user());
        if (request.alias() == null || request.alias().isBlank()) {
            throw new InvalidPersonException("No alias given");
        }
        body.set(new AliasRequest(request.alias().trim(), request.user()));
    }
}
