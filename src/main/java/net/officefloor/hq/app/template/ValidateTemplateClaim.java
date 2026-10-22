package net.officefloor.hq.app.template;

import net.officefloor.hq.app.claim.ClaimInput;
import net.officefloor.hq.app.claim.ClaimRequest;
import net.officefloor.hq.app.claim.InvalidClaimException;
import net.officefloor.hq.app.claim.ClaimFormRules;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

/** Turn a claim from a template into the claim form's add: the template's amount and note as they are now, no date,
 *  checked by the claim form's rules. A signed-in user and a template on the sheet are required. */
public class ValidateTemplateClaim {
    public void validate(@PathVariable("sheetId") String sheetId, @RequestBody TemplateClaimRequest request,
            TemplateRepository templates, Out<ClaimInput> body) throws InvalidClaimException {
        if (request == null || request.user() == null || request.user().isBlank()) {
            throw new InvalidClaimException("Sign in before changing claims.");
        }
        String name = request.template() == null ? "" : request.template().trim();
        if (name.isEmpty()) {
            throw new InvalidClaimException("Choose a template.");
        }
        Template template = templates.find(sheetId.trim(), name);
        if (template == null) {
            throw new InvalidClaimException("There is no template named " + name + ".");
        }
        body.set(ClaimFormRules.check(new ClaimRequest(request.user(), template.amount().setScale(2).toPlainString(),
                "", template.note(), "", "", "")));
    }
}
