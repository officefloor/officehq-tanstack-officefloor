package net.officefloor.hq.app.template;

import java.math.BigDecimal;
import net.officefloor.hq.app.claim.ClaimInput;
import net.officefloor.hq.app.claim.InvalidClaimException;
import net.officefloor.hq.app.setting.Settings;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * The claim a template makes: its amount and note as the template holds them now, dated like a claim added with no
 * date (the sheet's entry date; none set: its today). The claim then goes through every rule of the claim form.
 */
public class ValidateTemplateClaim {
    public void validate(@PathVariable("sheet") String sheet, @RequestBody TemplateClaimRequest request,
            ClaimTemplates templates, Settings settings, Out<ClaimInput> body) throws InvalidClaimException {
        String name = request == null || request.template() == null ? "" : request.template().trim();
        ClaimTemplate template = name.isEmpty() ? null : templates.find(sheet, name);
        if (template == null) {
            throw new InvalidClaimException("No template named " + name + " on the sheet");
        }
        body.set(new ClaimInput(new BigDecimal(template.amount()), settings.entryDay(sheet), template.note()));
    }
}
