package net.officefloor.hq.app.rate;

import java.util.ArrayList;
import java.util.List;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Reads the body: each non-blank line is grade,rate,from,to (from and to may be absent or blank), read by the rate
 *  form's rules (see ValidateRate). A failing line, or a paste with no lines, is refused. */
public class ValidateImportRates {
    public void validate(@RequestBody ImportRatesRequest request, Out<ImportedRates> imported) throws InvalidRateException {
        String text = request == null || request.text() == null ? "" : request.text();
        String by = request == null || request.by() == null ? "" : request.by().trim();
        List<RateValues> rates = new ArrayList<>();
        for (String line : text.split("\\r?\\n")) {
            if (line.isBlank()) {
                continue;
            }
            String[] fields = line.split(",", -1);
            if (fields.length > 4) {
                throw new InvalidRateException("A line is grade,rate,from,to");
            }
            rates.add(ValidateRate.check(fields[0], new RateRequest(fields.length > 1 ? fields[1] : "",
                    fields.length > 2 ? fields[2] : "", fields.length > 3 ? fields[3] : "", by)));
        }
        if (rates.isEmpty()) {
            throw new InvalidRateException("Paste at least one line of grade,rate,from,to");
        }
        imported.set(new ImportedRates(rates, by));
    }
}
