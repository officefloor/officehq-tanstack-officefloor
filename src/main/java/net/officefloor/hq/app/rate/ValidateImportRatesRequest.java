package net.officefloor.hq.app.rate;

import jakarta.validation.Valid;
import java.util.ArrayList;
import java.util.List;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Splits the text into lines of grade,rate,from,to (the from- and to-date may be absent or blank; blank lines are
 * skipped) and checks each with exactly the rate form's rules. Any line refused, or nothing to import, refuses the
 * whole import before anything is written.
 */
public class ValidateImportRatesRequest {
    public void validate(@RequestBody @Valid ImportRatesRequest request, Out<ImportedRates> imported) throws InvalidRateException {
        String text = request.text() == null ? "" : request.text();
        List<RateRequest> lines = new ArrayList<>();
        int number = 0;
        for (String line : text.split("\\r?\\n")) {
            number++;
            if (line.isBlank()) {
                continue;
            }
            String[] fields = line.split(",", -1);
            if (fields.length > 4) {
                throw new InvalidRateException("Line " + number + ": a line is grade,rate,from,to.");
            }
            // The rate form's field limits, which its request checks on binding.
            if (fields[0].length() > 10 || (fields.length > 1 && fields[1].length() > 30)
                    || (fields.length > 2 && fields[2].length() > 30) || (fields.length > 3 && fields[3].length() > 30)) {
                throw new InvalidRateException("Line " + number + ": a field is too long.");
            }
            RateRequest rate = new RateRequest(fields[0], fields.length > 1 ? fields[1] : "",
                    fields.length > 2 ? fields[2] : "", fields.length > 3 ? fields[3] : "");
            RateRequest[] valid = new RateRequest[1];
            try {
                new ValidateRateRequest().validate(rate, value -> valid[0] = value);
            } catch (InvalidRateException e) {
                throw new InvalidRateException("Line " + number + ": " + e.getMessage());
            }
            lines.add(valid[0]);
        }
        if (lines.isEmpty()) {
            throw new InvalidRateException("Paste at least one line of grade,rate,from,to.");
        }
        imported.set(new ImportedRates(lines));
    }
}
