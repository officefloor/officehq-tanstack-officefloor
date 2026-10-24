package net.officefloor.hq.app.template;

import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/** Saves the template on the sheet; saving a name again replaces that template's amount and note. */
public class ApplyTemplate {
    public void apply(@Val TemplateInput input, @Val Sheet sheet, ClaimTemplateRepository repository,
            Out<ClaimTemplate> templateOut) {
        ClaimTemplate template = repository.findBySheetIdAndName(sheet.getId(), input.name())
                .orElseGet(() -> new ClaimTemplate(sheet.getId(), input.name()));
        template.setAmount(input.amount());
        template.setNote(input.note());
        templateOut.set(repository.saveAndFlush(template));
    }
}
