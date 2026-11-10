package net.officefloor.hq.app.template;

import net.officefloor.hq.app.claim.ClaimInput;
import net.officefloor.hq.app.claim.ClaimRequest;
import net.officefloor.hq.app.claim.InvalidClaimException;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/**
 * The claim the template makes now: its amount and note as the template has them at this moment,
 * undated (so dated like a claim with no date), checked by the claim form's rules. A template not on
 * the sheet is refused.
 */
public class ClaimFromTemplate {
    public void build(@Val TemplateClaimRequest request, @Val Sheet sheet, TemplateRepository repository,
            Out<ClaimInput> claimOut) throws InvalidClaimException {
        ClaimTemplate template = repository.find(sheet.id(), request.template());
        if (template == null) throw new InvalidClaimException("No template " + request.template() + " on the sheet");
        claimOut.set(ClaimInput.from(new ClaimRequest(template.amount().toPlainString(), "", template.note(), "", "", "")));
    }
}
