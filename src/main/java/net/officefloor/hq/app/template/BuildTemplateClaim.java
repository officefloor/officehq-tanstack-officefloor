package net.officefloor.hq.app.template;

import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.claim.ClaimInput;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

/** The claim form as a template fills it, as the template stands now: its amount and note, a blank date
 *  (the sheet's entry date), nothing corrected, no reference and no tags. */
public class BuildTemplateClaim {
    public void build(@PathVariable("sheetId") String sheetId, @RequestBody TemplateClaimRequest request,
            TemplateRepository repository, Out<ClaimInput> claim) throws BadRequestException {
        String id = sheetId == null ? "" : sheetId.trim();
        String name = request == null || request.template() == null ? "" : request.template().trim();
        if (name.isEmpty()) {
            throw new BadRequestException("choose a template");
        }
        Template template = repository.find(id, name);
        if (template == null) {
            throw new BadRequestException("no such template");
        }
        claim.set(new ClaimInput(ClaimInput.amount(template.amount()), null, template.note(), null, null, null));
    }
}
