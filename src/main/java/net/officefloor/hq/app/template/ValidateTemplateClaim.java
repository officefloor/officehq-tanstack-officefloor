package net.officefloor.hq.app.template;

import net.officefloor.hq.app.claim.ClaimForm;
import net.officefloor.hq.app.claim.ClaimRequest;
import net.officefloor.hq.app.claim.InvalidClaimException;
import net.officefloor.hq.app.claim.ValidClaim;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

/** The claim a template makes now: its amount and note as they stand, no date (the sheet's default), checked
 *  by the claim form's rules. Refused when the sheet has no template of that name. */
public class ValidateTemplateClaim {
    public void validate(@PathVariable("sheetId") String sheetId, @RequestBody TemplateClaimRequest request,
            ClaimTemplateRepository templates, Out<ValidClaim> body) throws InvalidClaimException {
        String name = request == null || request.template() == null ? "" : request.template().trim();
        ClaimTemplate template = name.isEmpty() ? null : templates.find(sheetId.trim(), name);
        if (template == null) {
            throw new InvalidClaimException("No such template: " + name);
        }
        body.set(ClaimForm.check(new ClaimRequest(template.amount().toPlainString(), "", template.note())));
    }
}
