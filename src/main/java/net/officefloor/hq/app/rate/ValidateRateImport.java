package net.officefloor.hq.app.rate;

import java.util.ArrayList;
import java.util.List;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Bind the body and refuse it before anything is read or written. Each non-blank line is grade,rate,from,to — the
 *  from-date and to-date may be absent or blank — and must pass exactly the rate form's rules. A paste with no lines is
 *  refused. A refusal names the first line that fails. */
public class ValidateRateImport {
    public void validate(@RequestBody RateImportRequest request, Out<RateImportInput> body)
            throws InvalidRateException {
        if (request == null || request.user() == null || request.user().isBlank()) {
            throw new InvalidRateException("Sign in before changing rates.");
        }
        String user = request.user().trim();
        String[] rows = (request.text() == null ? "" : request.text()).split("\\r?\\n|\\r", -1);
        List<RateInput> rates = new ArrayList<>();
        for (int i = 0; i < rows.length; i++) {
            if (rows[i].isBlank()) {
                continue;
            }
            String[] fields = rows[i].split(",", -1);
            try {
                if (fields.length > 4) {
                    throw new InvalidRateException("A line is grade,rate,from,to.");
                }
                rates.add(RateRules.check(user, fields[0], new RateRequest(user, fields.length > 1 ? fields[1] : "",
                        fields.length > 2 ? fields[2] : "", fields.length > 3 ? fields[3] : "")));
            } catch (InvalidRateException e) {
                throw new InvalidRateException("Line " + (i + 1) + ": " + e.getMessage());
            }
        }
        if (rates.isEmpty()) {
            throw new InvalidRateException("Paste at least one line: grade,rate,from,to.");
        }
        body.set(new RateImportInput(user, rates));
    }
}
