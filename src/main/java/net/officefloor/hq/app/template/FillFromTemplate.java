package net.officefloor.hq.app.template;

import net.officefloor.hq.app.claim.ClaimInput;
import net.officefloor.hq.app.claim.ClaimRules;
import net.officefloor.hq.app.claim.InvalidClaimException;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/** Fills the claim form from the template as it is now: its amount and note, and no date (the sheet's today, or
 *  its entry date); refuses a template not on the sheet. */
public class FillFromTemplate {
    public void fill(@Val TemplateClaimRequest request, @Val Sheet sheet, ClaimTemplateRepository repository,
            Out<ClaimInput> claimOut) throws InvalidClaimException {
        ClaimTemplate template = repository.findBySheetIdAndName(sheet.getId(), request.template())
                .orElseThrow(() -> new InvalidClaimException("No such template: " + request.template()));
        claimOut.set(ClaimRules.check(template.getAmount().toPlainString(), null, template.getNote(), null, null));
    }
}
