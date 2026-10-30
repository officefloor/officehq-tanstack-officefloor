package net.officefloor.hq.app.statement;

import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PathVariable;
import net.officefloor.hq.app.setting.Settings;
import net.officefloor.hq.app.setting.ViewerRefusedException;

public class ValidateStatement {
    public void validate(@PathVariable("sheetId") String sheetId, @RequestBody StatementRequest request, Out<StatementRequest> body, Settings settings)
            throws ViewerRefusedException {
        settings.refuseViewer(sheetId, request.user());
        body.set(request);
    }
}
