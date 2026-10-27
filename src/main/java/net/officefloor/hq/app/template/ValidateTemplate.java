package net.officefloor.hq.app.template;

import net.officefloor.hq.app.claim.ClaimFormRules;
import net.officefloor.hq.app.claim.ClaimInput;
import net.officefloor.hq.app.claim.ClaimRequest;
import net.officefloor.hq.app.claim.InvalidClaimException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

public class ValidateTemplate {

    // The name, trimmed, must not be blank; the amount must be one the claim form takes for a claim with no date,
    // correction or reference. The note is kept trimmed, as the claim form keeps it.
    public void validate(@PathVariable("sheetId") String sheetId, @RequestBody TemplateRequest body,
            Out<ClaimTemplate> template) throws TemplateRefusedException {
        String name = body == null || body.name() == null ? "" : body.name().trim();
        if (name.isEmpty()) {
            throw new TemplateRefusedException("Give the template a name.");
        }
        ClaimInput input;
        try {
            input = ClaimFormRules.parse(new ClaimRequest(body.amount(), "", body.note(), "", "", ""));
        } catch (InvalidClaimException e) {
            throw new TemplateRefusedException(e.getMessage());
        }
        template.set(new ClaimTemplate(sheetId.trim(), name, input.amount(), input.note()));
    }
}
