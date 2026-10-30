package net.officefloor.hq.app.rate;

import java.util.ArrayList;
import java.util.List;
import net.officefloor.hq.app.setting.Settings;
import net.officefloor.hq.app.setting.ViewerRefusedException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Splits the import into lines of grade,rate,from,to (the dates may be absent or blank) and checks
 * each by the rate form's rules. Blank lines are skipped; an import with no rate in it is refused.
 */
public class ValidateImportRates {
    public void validate(@PathVariable("sheetId") String sheetId, @RequestBody ImportRatesRequest request, Out<ValidImportRates> body, Settings settings)
            throws InvalidRateException, ViewerRefusedException {
        settings.refuseViewer(sheetId, request.user());
        String text = request.text() == null ? "" : request.text();
        List<ValidRate> rates = new ArrayList<>();
        int number = 0;
        for (String raw : text.split("\\r?\\n")) {
            number++;
            if (raw.isBlank()) continue;
            String[] fields = raw.split(",", -1);
            if (fields.length < 2 || fields.length > 4) {
                throw new InvalidRateException("Line " + number + ": a line is grade,rate,from,to");
            }
            RateRequest rate = new RateRequest(fields[0], fields[1], fields.length > 2 ? fields[2] : "",
                    fields.length > 3 ? fields[3] : "", request.user());
            try {
                rates.add(ValidateRate.check(rate));
            } catch (InvalidRateException e) {
                throw new InvalidRateException("Line " + number + ": " + e.getMessage());
            }
        }
        if (rates.isEmpty()) {
            throw new InvalidRateException("Nothing to import: give one rate per line");
        }
        body.set(new ValidImportRates(rates, request.user()));
    }
}
