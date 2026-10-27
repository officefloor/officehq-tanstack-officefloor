package net.officefloor.hq.app.template;

import java.util.List;
import net.officefloor.hq.app.claim.ClaimInput;
import net.officefloor.hq.app.claim.InvalidClaimException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

public class ValidateTemplateClaim {

    // The claim takes the template's amount and note as they are now, and no date (the sheet's entry date, else its
    // today), correction, reference or tags; from here it is added by every rule of the claim form.
    public void validate(@PathVariable("sheetId") String sheetId, @RequestBody TemplateClaimRequest body,
            ClaimTemplates templates, Out<ClaimInput> input) throws InvalidClaimException {
        String name = body == null || body.template() == null ? "" : body.template().trim();
        ClaimTemplate template = name.isEmpty() ? null : templates.find(sheetId.trim(), name);
        if (template == null) {
            throw new InvalidClaimException("Choose a template.");
        }
        input.set(new ClaimInput(template.amount(), null, template.note(), null, null, List.of()));
    }
}
