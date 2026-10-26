package net.officefloor.hq.app.template;

import net.officefloor.plugin.variable.Val;

/** Saves the template, replacing one of the same name on the sheet. */
public class SaveTemplate {
    public void save(@Val Template template, TemplateRepository repository) {
        repository.save(template);
    }
}
