package net.officefloor.hq.app.template;

import java.math.BigDecimal;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

public class SaveTemplate {
    public void save(@PathVariable("sheetId") String sheetId,
            @RequestParam(name = "by", required = false) String by,
            @Val TemplateRequest request, SheetRepository sheets, ClaimTemplateRepository templates,
            Out<ClaimTemplate> template) {
        String id = sheetId.trim();
        String user = by == null ? "" : by;
        sheets.open(id, user);
        template.set(templates.save(id, request.name(), new BigDecimal(request.amount()), request.note(), user));
    }
}
