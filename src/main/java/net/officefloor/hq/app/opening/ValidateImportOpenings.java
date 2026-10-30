package net.officefloor.hq.app.opening;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import net.officefloor.hq.app.setting.Settings;
import net.officefloor.hq.app.setting.ViewerRefusedException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Splits the import into lines of name,period,amount and checks each is well formed: a name, a period
 * and an amount of 0.00 or more with at most two decimals. Blank lines are skipped; an import with no
 * line in it is refused.
 */
public class ValidateImportOpenings {

    private static final Pattern AMOUNT = Pattern.compile("\\d+(\\.\\d{1,2})?");

    public void validate(@PathVariable("sheetId") String sheetId, @RequestBody ImportOpeningsRequest request,
            Out<ValidImportOpenings> body, Settings settings) throws InvalidOpeningException, ViewerRefusedException {
        settings.refuseViewer(sheetId, request.user());
        String text = request.text() == null ? "" : request.text();
        List<ValidImportOpenings.Line> lines = new ArrayList<>();
        int number = 0;
        for (String raw : text.split("\\r?\\n")) {
            number++;
            if (raw.isBlank()) continue;
            String[] fields = raw.split(",", -1);
            if (fields.length != 3) {
                throw new InvalidOpeningException("Line " + number + ": a line is name,period,amount");
            }
            String name = fields[0].trim();
            String period = fields[1].trim();
            String amount = fields[2].trim();
            if (name.isEmpty() || period.isEmpty()) {
                throw new InvalidOpeningException("Line " + number + ": a name and a period are required");
            }
            if (!AMOUNT.matcher(amount).matches()) {
                throw new InvalidOpeningException("Line " + number + ": the amount must be 0.00 or more with at most two decimals");
            }
            lines.add(new ValidImportOpenings.Line(name, period, new BigDecimal(amount).setScale(2)));
        }
        if (lines.isEmpty()) {
            throw new InvalidOpeningException("Nothing to import: give one opening per line");
        }
        body.set(new ValidImportOpenings(lines, request.user()));
    }
}
