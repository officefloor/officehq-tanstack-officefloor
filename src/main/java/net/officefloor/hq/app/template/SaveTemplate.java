package net.officefloor.hq.app.template;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.CurrentUser;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import net.officefloor.server.http.ServerHttpConnection;
import org.springframework.web.bind.annotation.PathVariable;

public class SaveTemplate {
    public void save(@PathVariable("sheetId") String sheetId, @Val TemplateInput input,
            ServerHttpConnection connection, TemplateRepository repository, Audit audit, Out<Template> template)
            throws BadRequestException {
        String by = CurrentUser.require(connection);
        String id = sheetId == null ? "" : sheetId.trim();
        if (!repository.sheetExists(id)) {
            throw new BadRequestException("no such sheet");
        }
        Template saved = repository.save(id, input.name(), input.amount(), input.note());
        audit.record("TEMPLATE_SAVED sheet=" + id + " by=" + by + " name=" + saved.name() + " amount=" + saved.amount()
                + " note=" + saved.note());
        template.set(saved);
    }
}
