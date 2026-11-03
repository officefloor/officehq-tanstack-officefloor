package net.officefloor.hq.app.template;

import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.CurrentUser;
import net.officefloor.plugin.variable.Out;
import net.officefloor.server.http.ServerHttpConnection;
import org.springframework.web.bind.annotation.PathVariable;

public class LoadTemplates {
    public void load(@PathVariable("sheetId") String sheetId, ServerHttpConnection connection,
            TemplateRepository repository, Out<Templates> templates) throws BadRequestException {
        CurrentUser.require(connection);
        String id = sheetId == null ? "" : sheetId.trim();
        templates.set(new Templates(repository.list(id)));
    }
}
