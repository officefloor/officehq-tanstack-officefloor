package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.setting.Settings;
import net.officefloor.hq.app.setting.ViewerRefusedException;
import net.officefloor.hq.app.template.ClaimTemplate;
import net.officefloor.hq.app.template.ClaimTemplates;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

/** A claim from a template is the claim form with the template's amount and note as they are now, and no date
 *  (so it takes the sheet's claim date); it is then saved by every rule of the claim form. */
public class ValidateTemplateClaim {
    public void validate(@PathVariable("sheetId") String sheetId, @RequestBody TemplateClaimRequest request,
            Out<ValidClaim> body, Settings settings, ClaimTemplates templates) throws InvalidClaimException, ViewerRefusedException {
        settings.refuseViewer(sheetId, request.user());
        String name = request.template() == null ? "" : request.template().trim();
        ClaimTemplate template = templates.find(sheetId, name)
                .orElseThrow(() -> new InvalidClaimException("There is no template " + name));
        body.set(ValidateClaim.check(new ClaimRequest(template.amount().toPlainString(), "", template.note(), request.user(), "", "", "")));
    }
}
