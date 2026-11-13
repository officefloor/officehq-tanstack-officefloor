package net.officefloor.hq.app.template;

import jakarta.validation.Valid;
import net.officefloor.hq.app.claim.ClaimInput;
import net.officefloor.hq.app.claim.ClaimRequest;
import net.officefloor.hq.app.claim.InvalidClaimException;
import net.officefloor.hq.app.claim.ClaimForm;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

/** The claim a template makes now: its amount and note as they are at this moment, with no date (so dated as the
 * claim form dates a claim with none). A template not on the sheet is refused (400). */
public class ValidateTemplateClaimRequest {
    public void validate(@PathVariable("sheetId") String sheetId, @RequestBody @Valid TemplateClaimRequest request,
            TemplateRepository templates, Out<ClaimInput> input) throws InvalidClaimException {
        String name = request == null || request.template() == null ? "" : request.template().trim();
        ClaimTemplate template = name.isEmpty() ? null : templates.find(sheetId, name);
        if (template == null) {
            throw new InvalidClaimException("Choose a claim template.");
        }
        input.set(ClaimForm.check(new ClaimRequest(template.amount().setScale(2).toPlainString(), "",
                template.note(), "", "", "")));
    }
}
