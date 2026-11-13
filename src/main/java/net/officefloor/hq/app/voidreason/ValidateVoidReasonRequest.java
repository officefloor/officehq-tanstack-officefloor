package net.officefloor.hq.app.voidreason;

import jakarta.validation.Valid;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestBody;

/** Refuses (400) a void reason that is blank (once trimmed) or already on the sheet's list. */
public class ValidateVoidReasonRequest {
    public void validate(@Val Sheet sheet, @RequestBody @Valid VoidReasonRequest request,
            VoidReasonRepository reasons, Out<VoidReason> reason) throws InvalidVoidReasonException {
        String text = request == null || request.reason() == null ? "" : request.reason().trim();
        if (text.isEmpty()) {
            throw new InvalidVoidReasonException("A void reason cannot be blank.");
        }
        if (reasons.findAll(sheet.id()).contains(text)) {
            throw new InvalidVoidReasonException(text + " is already a void reason.");
        }
        reason.set(new VoidReason(sheet.id(), text));
    }
}
