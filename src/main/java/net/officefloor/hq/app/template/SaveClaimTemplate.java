package net.officefloor.hq.app.template;

import net.officefloor.plugin.variable.Val;

public class SaveClaimTemplate {
    public void save(@Val ClaimTemplate template, ClaimTemplates templates) {
        templates.save(template);
    }
}
