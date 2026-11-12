package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.role.RoleStore;
import net.officefloor.hq.app.sheet.InvalidSheetException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

public class ValidateComment {
    public void validate(@PathVariable("sheetId") String sheetId, @RequestBody CommentRequest request, Out<CommentInput> body, RoleStore roles) throws InvalidSheetException {
        roles.refuseChange(sheetId.trim(), request == null ? null : request.user());
        String text = request == null || request.text() == null ? "" : request.text().trim();
        if (text.isEmpty()) {
            throw new InvalidSheetException("A comment needs some text");
        }
        body.set(new CommentInput(request.user() == null ? "" : request.user().trim(), text));
    }
}
