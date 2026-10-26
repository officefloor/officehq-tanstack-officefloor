package net.officefloor.hq.app.voidreason;

import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

public class ValidateVoidReason {

    // The reason, trimmed, must not be blank, nor already on the sheet's list (without regard to case).
    public void validate(@PathVariable("sheetId") String sheetId, @RequestBody VoidReasonRequest body,
            VoidReasons reasons, Out<VoidReason> reason) throws VoidReasonRefusedException {
        String sheet = sheetId.trim();
        String text = body == null || body.reason() == null ? "" : body.reason().trim();
        if (text.isEmpty()) {
            throw new VoidReasonRefusedException("Give the void reason.");
        }
        if (reasons.has(sheet, text)) {
            throw new VoidReasonRefusedException("\"" + text + "\" is already a void reason.");
        }
        reason.set(new VoidReason(sheet, text));
    }
}
