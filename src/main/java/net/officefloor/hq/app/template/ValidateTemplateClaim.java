package net.officefloor.hq.app.template;

import net.officefloor.hq.app.claim.ClaimRequest;
import net.officefloor.hq.app.claim.ClaimValues;
import net.officefloor.hq.app.rollup.Actor;
import net.officefloor.hq.app.sheet.InvalidSheetRequestException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

/** The claim a template makes, as the claim form would send it at this moment: the template's
 *  amount and note, no date (the sheet's today), no correction, reference or tags. */
public class ValidateTemplateClaim {
    public void validate(@PathVariable("sheetId") String sheetId, @RequestBody TemplateClaimRequest request,
            ClaimTemplates templates, Out<ClaimValues> body, Out<Actor> actor) throws InvalidSheetRequestException {
        String name = request == null || request.template() == null ? "" : request.template().trim();
        ClaimTemplate template = templates.named(sheetId, name)
                .orElseThrow(() -> new InvalidSheetRequestException("No such claim template: " + name));
        ClaimValues values = ClaimRequest.checked(new ClaimRequest(template.amount().toPlainString(), "",
                template.note(), "", request.user(), "", ""));
        actor.set(new Actor(values.user()));
        body.set(values);
    }
}
