package net.officefloor.hq.app.period;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import net.officefloor.hq.app.rollup.Actor;
import net.officefloor.hq.app.sheet.InvalidSheetRequestException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Splits the paste into lines of name,period,amount and checks each line's form: a name, a period
 *  and an amount of two decimals, 0.00 or more; blank lines are skipped. One refused line refuses
 *  the whole paste. */
public class ValidateImportOpenings {
    private static final Pattern AMOUNT = Pattern.compile("\\d+\\.\\d{2}");

    public void validate(@RequestBody ImportOpeningsRequest request, Out<ImportedOpenings> body, Out<Actor> actor)
            throws InvalidSheetRequestException {
        String user = request == null ? null : request.user();
        String text = request == null || request.text() == null ? "" : request.text();
        List<OpeningLine> lines = new ArrayList<>();
        int n = 0;
        for (String raw : text.split("\\r?\\n")) {
            n++;
            if (raw.isBlank()) continue;
            String[] f = raw.split(",", -1);
            if (f.length != 3 || f[0].isBlank() || f[1].isBlank()) {
                throw new InvalidSheetRequestException("Line " + n + ": expected name,period,amount");
            }
            String amount = f[2].trim();
            if (!AMOUNT.matcher(amount).matches()) {
                throw new InvalidSheetRequestException("Line " + n + ": the amount must have two decimals and be 0.00 or more");
            }
            lines.add(new OpeningLine(n, f[0].trim(), f[1].trim(), new BigDecimal(amount)));
        }
        if (lines.isEmpty()) throw new InvalidSheetRequestException("Paste at least one line of name,period,amount");
        actor.set(new Actor(user));
        body.set(new ImportedOpenings(lines));
    }
}
