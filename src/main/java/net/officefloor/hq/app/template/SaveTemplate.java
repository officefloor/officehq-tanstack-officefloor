package net.officefloor.hq.app.template;

import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

/** Store the template, replacing one of the same name on the sheet. */
public class SaveTemplate {
    public void save(@PathVariable("sheetId") String sheetId, @Val TemplateInput input, PeriodRepository periods,
            TemplateRepository repository, Out<Template> template) {
        String id = sheetId.trim();
        periods.lockSheet(id);
        template.set(repository.save(id, input));
    }
}
