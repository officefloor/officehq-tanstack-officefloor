package net.officefloor.hq.app.person;

import net.officefloor.hq.app.setting.Settings;
import net.officefloor.hq.app.setting.ViewerRefusedException;
import net.officefloor.hq.app.sheet.Sheets;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

public class ValidateMove {
    public void validate(@PathVariable("sheetId") String sheetId, @RequestBody MoveRequest request, Out<MoveRequest> body,
            Sheets sheets, Settings settings) throws InvalidPersonException, ViewerRefusedException {
        if (sheets.archived(sheetId)) {
            throw new InvalidPersonException("The sheet is archived: nothing on it can change");
        }
        settings.refuseViewer(sheetId, request.user()); // a viewer changes nothing
        body.set(request);
    }
}
