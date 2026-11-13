package net.officefloor.hq.app.template;

import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

public class LoadTemplates {
    public void load(@Val Sheet sheet, TemplateRepository repository, Out<ClaimTemplates> templates) {
        templates.set(new ClaimTemplates(repository.findAll(sheet.id())));
    }
}
