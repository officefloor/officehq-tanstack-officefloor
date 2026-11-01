package net.officefloor.hq.app.template;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.regex.Pattern;
import net.officefloor.hq.app.Audit;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Saves a claim template (name and note trimmed), replacing one of the same name; refused for a blank name or an
 * amount the claim form would refuse (not a number with up to two decimals, or negative: a template names no claim
 * it corrects).
 */
public class SaveTemplate {
    private static final Pattern AMOUNT = Pattern.compile("\\d+(\\.\\d{1,2})?");

    public void save(@PathVariable("sheet") String sheet, @RequestParam("user") String user,
            @RequestBody TemplateRequest request, ClaimTemplates templates, Audit audit) throws InvalidTemplateException {
        String name = request == null || request.name() == null ? "" : request.name().trim();
        String amount = request == null || request.amount() == null ? "" : request.amount().trim();
        String note = request == null || request.note() == null ? "" : request.note().trim();
        if (name.isEmpty()) {
            throw new InvalidTemplateException("A template must have a name");
        }
        if (!AMOUNT.matcher(amount).matches()) {
            throw new InvalidTemplateException("The amount must be a number with up to two decimals, not negative");
        }
        BigDecimal value = new BigDecimal(amount).setScale(2, RoundingMode.UNNECESSARY);
        templates.save(sheet, name, value, note);
        audit.record("TEMPLATE_SAVED sheet=" + sheet + " by=" + user + " name=" + name + " amount="
                + value.toPlainString() + " note=" + note);
    }
}
