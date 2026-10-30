package net.officefloor.hq.app.claim;

import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PathVariable;
import net.officefloor.hq.app.setting.Settings;
import net.officefloor.hq.app.setting.ViewerRefusedException;

public class ValidateReassign {
    public void validate(@PathVariable("sheetId") String sheetId, @RequestBody ReassignRequest request, Out<ReassignRequest> body, Settings settings)
            throws InvalidClaimException, ViewerRefusedException {
        settings.refuseViewer(sheetId, request.user());
        if (request.toPersonId() == null || request.toPersonId().isBlank()) {
            throw new InvalidClaimException("No person chosen to move the claim to");
        }
        body.set(request);
    }
}
