package net.officefloor.hq.app.person;

import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PathVariable;
import net.officefloor.hq.app.setting.Settings;
import net.officefloor.hq.app.setting.ViewerRefusedException;

public class ValidateTransfer {
    public void validate(@PathVariable("sheetId") String sheetId, @RequestBody TransferRequest request, Out<TransferRequest> body, Settings settings)
            throws InvalidPersonException, ViewerRefusedException {
        settings.refuseViewer(sheetId, request.user());
        String to = request.toSheet() == null ? "" : request.toSheet().trim();
        if (to.isEmpty()) {
            throw new InvalidPersonException("No sheet chosen to transfer to");
        }
        if (to.equals(sheetId)) {
            throw new InvalidPersonException("A person cannot be transferred to their own sheet");
        }
        body.set(new TransferRequest(to, request.user()));
    }
}
