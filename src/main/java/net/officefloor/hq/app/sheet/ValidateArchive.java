package net.officefloor.hq.app.sheet;

import net.officefloor.hq.app.rollup.Actor;
import net.officefloor.hq.app.settings.Role;
import net.officefloor.hq.app.settings.Settings;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

/** Only a steward may archive the sheet. */
public class ValidateArchive {
    public void validate(@PathVariable("sheetId") String sheetId, @RequestBody ArchiveRequest request,
            Settings settings, Out<Actor> actor) throws InvalidSheetRequestException {
        String user = request == null || request.user() == null ? "" : request.user().trim();
        if (user.isEmpty() || settings.role(sheetId, user) != Role.STEWARD) {
            throw new InvalidSheetRequestException("Only a steward can archive the sheet");
        }
        actor.set(new Actor(user));
    }
}
