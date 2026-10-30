package net.officefloor.hq.app.voidreason;

import net.officefloor.hq.app.setting.Settings;
import net.officefloor.hq.app.setting.ViewerRefusedException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

public class ValidateVoidReason {
    public void validate(@PathVariable("sheetId") String sheetId, @RequestBody VoidReasonRequest request,
            Out<ValidVoidReason> body, Settings settings, VoidReasons reasons)
            throws InvalidVoidReasonException, ViewerRefusedException {
        settings.refuseViewer(sheetId, request.user());
        String reason = request.reason() == null ? "" : request.reason().trim();
        if (reason.isEmpty()) {
            throw new InvalidVoidReasonException("A void reason cannot be blank");
        }
        if (reasons.contains(sheetId, reason)) {
            throw new InvalidVoidReasonException("The reason is already on the list");
        }
        body.set(new ValidVoidReason(reason, request.user()));
    }
}
