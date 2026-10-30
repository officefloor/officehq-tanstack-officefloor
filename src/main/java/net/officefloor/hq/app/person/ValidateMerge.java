package net.officefloor.hq.app.person;

import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PathVariable;
import net.officefloor.hq.app.setting.Settings;
import net.officefloor.hq.app.setting.ViewerRefusedException;

public class ValidateMerge {
    public void validate(@PathVariable("sheetId") String sheetId, @RequestBody MergeRequest request, Out<MergeRequest> body, Settings settings)
            throws InvalidPersonException, ViewerRefusedException {
        settings.refuseViewer(sheetId, request.user());
        if (request.intoId() == null || request.intoId().isBlank()) {
            throw new InvalidPersonException("No person chosen to merge into");
        }
        body.set(request);
    }
}
