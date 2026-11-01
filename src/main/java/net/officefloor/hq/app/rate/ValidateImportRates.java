package net.officefloor.hq.app.rate;

import java.util.ArrayList;
import java.util.List;
import net.officefloor.hq.app.grade.Grades;
import net.officefloor.hq.app.rollup.Actor;
import net.officefloor.hq.app.sheet.InvalidSheetRequestException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

/** Splits the import into lines of grade,rate,from,to (the last two may be absent or blank) and checks
 *  each by the rate form's rules; blank lines are skipped. One refused line refuses the whole import. */
public class ValidateImportRates {
    public void validate(@PathVariable("sheetId") String sheetId, @RequestBody ImportRatesRequest request,
            Grades grades, Out<ImportedRates> body, Out<Actor> actor) throws InvalidSheetRequestException {
        String user = request == null ? null : request.user();
        String text = request == null || request.text() == null ? "" : request.text();
        List<RateRequest> requests = new ArrayList<>();
        List<Rate> rates = new ArrayList<>();
        int n = 0;
        for (String raw : text.split("\\r?\\n")) {
            n++;
            if (raw.isBlank()) continue;
            String[] f = raw.split(",", -1);
            if (f.length < 2 || f.length > 4) {
                throw new InvalidSheetRequestException("Line " + n + ": expected grade,rate,from,to");
            }
            RateRequest line = new RateRequest(f[0].trim(), f[1].trim(), f.length > 2 ? f[2].trim() : "",
                    f.length > 3 ? f[3].trim() : "", user);
            try {
                rates.add(ValidateRate.checked(sheetId, line, grades));
            } catch (InvalidSheetRequestException e) {
                throw new InvalidSheetRequestException("Line " + n + ": " + e.getMessage());
            }
            requests.add(line);
        }
        if (requests.isEmpty()) throw new InvalidSheetRequestException("Paste at least one line of grade,rate,from,to");
        actor.set(new Actor(user));
        body.set(new ImportedRates(requests, rates));
    }
}
