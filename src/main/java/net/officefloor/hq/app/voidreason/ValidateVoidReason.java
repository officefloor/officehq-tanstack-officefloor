package net.officefloor.hq.app.voidreason;

import net.officefloor.hq.app.sheet.InvalidSheetRequestException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

/** Refuses a void reason that is blank, or already on the sheet's list (its text trimmed). */
public class ValidateVoidReason {
    public void validate(@PathVariable("sheetId") String sheetId, @RequestBody VoidReasonRequest request,
            VoidReasons reasons, Out<VoidReason> reason) throws InvalidSheetRequestException {
        String text = request == null || request.reason() == null ? "" : request.reason().trim();
        if (text.isEmpty()) {
            throw new InvalidSheetRequestException("A void reason cannot be blank");
        }
        if (text.length() > 200) {
            throw new InvalidSheetRequestException("A void reason is at most 200 characters");
        }
        if (reasons.has(sheetId, text)) {
            throw new InvalidSheetRequestException("Already a void reason: " + text);
        }
        reason.set(new VoidReason(sheetId, text, request.user()));
    }
}
