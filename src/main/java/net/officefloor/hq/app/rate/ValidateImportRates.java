package net.officefloor.hq.app.rate;

import java.util.ArrayList;
import java.util.List;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Splits the import into its lines (blank lines skipped) and checks each with the rate form's own
 *  rules; an import with no lines, or any line refused, is refused. */
public class ValidateImportRates {
    public void validate(@RequestBody ImportRatesRequest request, Out<ImportRateLines> lines)
            throws InvalidRateException {
        String text = request == null || request.text() == null ? "" : request.text();
        List<ValidRate> checked = new ArrayList<>();
        for (String line : text.split("\\r?\\n")) {
            if (line.isBlank()) continue;
            String[] f = line.split(",", -1);
            if (f.length > 4) {
                throw new InvalidRateException("Each line is grade,rate,from,to: " + line);
            }
            checked.add(RateForm.check(new RateRequest(f[0], f.length > 1 ? f[1] : "",
                    f.length > 2 ? f[2] : "", f.length > 3 ? f[3] : "")));
        }
        if (checked.isEmpty()) {
            throw new InvalidRateException("Import at least one line of grade,rate,from,to");
        }
        lines.set(new ImportRateLines(checked));
    }
}
