package net.officefloor.hq.app.template;

import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/** Saves the template, replacing the sheet's template of the same name. */
public class SaveTemplate {
    public void save(@Val TemplateInput input, @Val Sheet sheet, TemplateRepository repository,
            Out<ClaimTemplate> templateOut) {
        ClaimTemplate template = new ClaimTemplate(sheet.id(), input.name(), input.amount(), input.note());
        repository.save(template);
        templateOut.set(template);
    }
}
