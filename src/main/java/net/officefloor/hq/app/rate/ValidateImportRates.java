package net.officefloor.hq.app.rate;

import java.util.ArrayList;
import java.util.List;
import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.session.ChangedBy;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * The rate import box's lines of grade,rate,from,to (from and to may be absent or blank; blank lines are skipped).
 * Each line is checked with the rate form's rules; a refusal names the line. Publishes them parsed, in order, with the
 * user making the change.
 */
public class ValidateImportRates {

    static final int MAX_LINES = 1000;

    public void validate(@RequestBody ImportRatesRequest request, Out<ImportedRates> imported,
            Out<ChangedBy> changedBy) throws BadRequestException {
        ChangedBy by = ChangedBy.of(request == null ? null : request.by());
        String text = request == null || request.lines() == null ? "" : request.lines();
        List<RateInput> rates = new ArrayList<>();
        String[] typed = text.split("\\r?\\n");
        for (int i = 0; i < typed.length; i++) {
            if (typed[i].trim().isEmpty()) {
                continue;
            }
            String[] fields = typed[i].split(",", -1);
            if (fields.length < 2 || fields.length > 4) {
                throw new BadRequestException("Line " + (i + 1) + ": enter grade,rate,from,to");
            }
            try {
                rates.add(ValidateRate.parse(new RateRequest(fields[0], fields[1],
                        fields.length > 2 ? fields[2] : "", fields.length > 3 ? fields[3] : "", by.name())));
            } catch (BadRequestException e) {
                throw new BadRequestException("Line " + (i + 1) + ": " + e.getMessage());
            }
        }
        if (rates.isEmpty()) {
            throw new BadRequestException("Paste at least one line of grade,rate,from,to");
        }
        if (rates.size() > MAX_LINES) {
            throw new BadRequestException("Import at most " + MAX_LINES + " lines at once");
        }
        imported.set(new ImportedRates(rates));
        changedBy.set(by);
    }
}
