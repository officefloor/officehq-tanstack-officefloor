package net.officefloor.hq.app.importrates;

import java.util.ArrayList;
import java.util.List;
import net.officefloor.hq.app.rate.InvalidRateException;
import net.officefloor.hq.app.rate.RateInput;
import net.officefloor.hq.app.rate.RateRequest;
import net.officefloor.hq.app.rate.RateRules;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Reads the rate import box from the body: each non-blank line is grade,rate,from,to — the from-date and the to-date
 * may be blank or absent. Checks every line exactly as the rate form would, refusing the whole import when any line
 * fails or there is no line.
 */
public class ValidateImportRates {
    public void validate(@RequestBody ImportRatesRequest request, Out<ImportedRates> ratesOut)
            throws InvalidRateException {
        String text = request == null || request.text() == null ? "" : request.text();
        List<RateInput> rates = new ArrayList<>();
        int at = 0;
        for (String line : text.split("\\r?\\n")) {
            at++;
            if (line.isBlank()) {
                continue;
            }
            String[] fields = line.split(",", -1);
            if (fields.length < 2 || fields.length > 4) {
                throw new InvalidRateException("Line " + at + ": a line is grade,rate,from,to.");
            }
            try {
                rates.add(RateRules.check(new RateRequest(fields[0], fields[1],
                        fields.length > 2 ? fields[2] : null, fields.length > 3 ? fields[3] : null)));
            } catch (InvalidRateException e) {
                throw new InvalidRateException("Line " + at + ": " + e.getMessage());
            }
        }
        if (rates.isEmpty()) {
            throw new InvalidRateException("Paste at least one line of grade,rate,from,to.");
        }
        ratesOut.set(new ImportedRates(List.copyOf(rates)));
    }
}
