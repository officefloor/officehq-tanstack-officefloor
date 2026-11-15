package net.officefloor.hq.app.voidreason;

import net.officefloor.hq.app.setting.InvalidSettingException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

/** Refused for a blank reason, and for one already on the sheet's list (compared trimmed). */
public class ValidateVoidReason {
    public void validate(@PathVariable("sheetId") String sheetId, @RequestBody VoidReasonRequest request,
            VoidReasonRepository reasons, Out<VoidReasonRequest> body) throws InvalidSettingException {
        String text = request == null || request.reason() == null ? "" : request.reason().trim();
        if (text.isEmpty()) {
            throw new InvalidSettingException("A void reason needs text");
        }
        if (reasons.contains(sheetId.trim(), text)) {
            throw new InvalidSettingException("Already a void reason: " + text);
        }
        body.set(new VoidReasonRequest(text));
    }
}
