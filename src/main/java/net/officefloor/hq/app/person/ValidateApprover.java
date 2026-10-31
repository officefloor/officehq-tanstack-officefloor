package net.officefloor.hq.app.person;

import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import net.officefloor.hq.app.setting.Settings;
import net.officefloor.hq.app.setting.ViewerRefusedException;

/** A viewer may not set an approver; the user name is trimmed (blank clears it) and has no spaces. */
public class ValidateApprover {
    public void validate(@PathVariable("sheetId") String sheetId, @RequestBody ApproverRequest request,
            Out<ValidApprover> body, Settings settings) throws InvalidPersonException, ViewerRefusedException {
        settings.refuseViewer(sheetId, request.user());
        String approver = request.approver() == null ? "" : request.approver().trim();
        if (approver.chars().anyMatch(Character::isWhitespace) || approver.length() > 255) {
            throw new InvalidPersonException("An approver is a single user name");
        }
        body.set(new ValidApprover(approver, request.user()));
    }
}
