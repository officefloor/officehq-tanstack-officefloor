package net.officefloor.hq.app.template;

import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

/** Stores the template on the sheet, replacing the one already saved under its name. */
public class SaveTemplate {
    public void save(@PathVariable("sheetId") String sheetId, @Val TemplateValues values, TemplateRepository templates) {
        templates.save(sheetId.trim(), values.template());
    }
}
