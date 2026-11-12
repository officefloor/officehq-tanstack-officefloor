package net.officefloor.hq.app.template;

import java.math.BigDecimal;
import java.util.regex.Pattern;
import net.officefloor.hq.app.role.RoleStore;
import net.officefloor.hq.app.sheet.InvalidSheetException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

/** Refuses a template by a viewer, with a blank name, or with an amount the claim form would refuse for a
 *  claim made from it (one that is not a number with up to two decimals, or a negative one: a template
 *  names no claim to correct). */
public class ValidateTemplate {
    private static final Pattern AMOUNT = Pattern.compile("\\d+(\\.\\d{1,2})?");

    public void validate(@PathVariable("sheetId") String sheetId, @RequestBody TemplateRequest request,
            Out<TemplateInput> body, RoleStore roles) throws InvalidSheetException {
        String user = request == null || request.user() == null ? "" : request.user().trim();
        roles.refuseChange(sheetId.trim(), user);
        String name = request == null || request.name() == null ? "" : request.name().trim();
        if (name.isEmpty()) {
            throw new InvalidSheetException("A template needs a name");
        }
        String amount = request.amount() == null ? "" : request.amount().trim();
        if (!AMOUNT.matcher(amount).matches()) {
            throw new InvalidSheetException("The amount must be a number with up to two decimals");
        }
        String note = request.note() == null ? "" : request.note().trim();
        body.set(new TemplateInput(new Template(name, new BigDecimal(amount).setScale(2), note), user));
    }
}
