package net.officefloor.hq.app.template;

import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

public class SaveTemplate {
    public void save(@PathVariable("sheetId") String sheetId, @Val TemplateInput input, TemplateStore store) {
        store.save(sheetId.trim(), input.template());
    }
}
