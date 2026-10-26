package net.officefloor.hq.app.template;

import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.claim.ClaimInput;
import net.officefloor.hq.app.sheet.SheetResponse;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/**
 * The claim form's fields a template gives as it stands now: its amount and note, no date (the claim is dated as one
 * entered with none). The claim is then added by every rule of the claim form.
 */
public class ClaimFromTemplate {
    public void fill(@Val SheetResponse sheet, @Val TemplateName name, TemplateRepository repository,
            Out<ClaimInput> claim) throws BadRequestException {
        Template template = repository.find(sheet.id(), name.name());
        if (template == null) {
            throw new BadRequestException("No such template on this sheet");
        }
        claim.set(new ClaimInput(template.amount(), null, template.note()));
    }
}
