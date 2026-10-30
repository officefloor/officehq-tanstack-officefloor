package net.officefloor.hq.app.person;

import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PathVariable;
import net.officefloor.hq.app.setting.Settings;
import net.officefloor.hq.app.setting.ViewerRefusedException;

public class ValidateTransferUndo {
    public void validate(@PathVariable("sheetId") String sheetId, @RequestBody TransferUndoRequest request,
            Out<TransferUndoRequest> body, Settings settings) throws ViewerRefusedException {
        settings.refuseViewer(sheetId, request.user());
        body.set(request);
    }
}
