package net.officefloor.hq.app.template;

import java.util.List;
import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;

/** Saves the template (replacing one of the same name); claims already made from it keep their amount and note. */
public class SaveTemplate {
    public void save(@PathVariable("sheetId") String sheetId, @Val ValidTemplate template, ClaimTemplates templates, Audit audit,
            ObjectResponse<List<TemplateResponse>> response) {
        templates.save(sheetId, new ClaimTemplate(template.name(), template.amount(), template.note()));
        audit.recordOnCommit("TEMPLATE_SAVED sheet=" + sheetId + " by=" + template.user() + " name=" + template.name()
                + " amount=" + template.amount().toPlainString() + " note=" + template.note());
        response.send(templates.list(sheetId).stream().map(TemplateResponse::from).toList());
    }
}
