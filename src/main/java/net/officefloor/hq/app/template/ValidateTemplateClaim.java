package net.officefloor.hq.app.template;

import net.officefloor.hq.app.claim.ClaimValues;
import net.officefloor.hq.app.claim.InvalidClaimException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

/** Reads the body and takes the named template's amount and note as they are now, as the claim's values with no date
 *  (so it is dated like a claim given none); a template not on the sheet is refused. */
public class ValidateTemplateClaim {
    public void validate(@PathVariable("sheetId") String sheetId, @RequestBody TemplateClaimRequest request,
            TemplateRepository templates, Out<ClaimValues> values) throws InvalidClaimException {
        String name = request == null || request.template() == null ? "" : request.template().trim();
        Template template = name.isEmpty() ? null : templates.find(sheetId.trim(), name);
        if (template == null) {
            throw new InvalidClaimException("No template " + name);
        }
        values.set(new ClaimValues(template.amount(), null, template.note(), null,
                request.by() == null ? "" : request.by().trim(), null, null));
    }
}
