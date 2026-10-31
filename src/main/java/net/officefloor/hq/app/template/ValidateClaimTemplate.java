package net.officefloor.hq.app.template;

import net.officefloor.hq.app.claim.ClaimRequest;
import net.officefloor.hq.app.claim.ClaimValues;
import net.officefloor.hq.app.sheet.InvalidSheetRequestException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

/** Refuses a template with a blank name, or an amount the claim form would refuse (it names no correction). */
public class ValidateClaimTemplate {
    public void validate(@PathVariable("sheetId") String sheetId, @RequestBody ClaimTemplateRequest request,
            Out<ClaimTemplate> template) throws InvalidSheetRequestException {
        String name = request == null || request.name() == null ? "" : request.name().trim();
        if (name.isEmpty()) {
            throw new InvalidSheetRequestException("A template name cannot be blank");
        }
        if (name.length() > 200) {
            throw new InvalidSheetRequestException("A template name is at most 200 characters");
        }
        String note = request.note() == null ? "" : request.note();
        ClaimValues values = ClaimRequest.checked(new ClaimRequest(request.amount(), "", note, "", request.user(), "", ""));
        template.set(new ClaimTemplate(sheetId, name, values.amount(), note, request.user()));
    }
}
