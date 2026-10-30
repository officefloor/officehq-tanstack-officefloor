package net.officefloor.hq.app.claim;

import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PathVariable;
import net.officefloor.hq.app.setting.Settings;
import net.officefloor.hq.app.setting.ViewerRefusedException;

/** A comment needs some text (an empty or blank one is refused); a viewer, or any user on an archived sheet, is refused. */
public class ValidateComment {
    public void validate(@PathVariable("sheetId") String sheetId, @RequestBody CommentRequest request, Out<CommentRequest> body,
            Settings settings) throws ViewerRefusedException, InvalidClaimException {
        settings.refuseViewer(sheetId, request.user());
        if (request.text() == null || request.text().isBlank()) {
            throw new InvalidClaimException("A comment needs some text");
        }
        body.set(request);
    }
}
