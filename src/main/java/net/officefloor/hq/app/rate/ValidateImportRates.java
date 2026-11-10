package net.officefloor.hq.app.rate;

import java.util.ArrayList;
import java.util.List;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Splits the import into lines (blank lines skipped) and checks each by the rate form's rules; the
 * first line that fails refuses the whole import. An import with no lines is refused too.
 */
public class ValidateImportRates {
    public void validate(@RequestBody ImportRatesRequest request, Out<ImportRatesLines> linesOut)
            throws InvalidRateException {
        String text = request == null || request.text() == null ? "" : request.text();
        List<RateInput> lines = new ArrayList<>();
        int number = 0;
        for (String raw : text.split("\\r?\\n|\\r")) {
            number++;
            if (raw.isBlank()) continue;
            String[] fields = raw.split(",", -1);
            if (fields.length < 2 || fields.length > 4) {
                throw new InvalidRateException("Line " + number + ": grade,rate,from,to");
            }
            String from = fields.length > 2 ? fields[2] : "";
            String to = fields.length > 3 ? fields[3] : "";
            try {
                lines.add(RateRequest.checked(new RateRequest(fields[0], fields[1], from, to)));
            } catch (InvalidRateException e) {
                throw new InvalidRateException("Line " + number + ": " + e.getMessage());
            }
        }
        if (lines.isEmpty()) throw new InvalidRateException("Paste at least one line of grade,rate,from,to");
        linesOut.set(new ImportRatesLines(lines));
    }
}
