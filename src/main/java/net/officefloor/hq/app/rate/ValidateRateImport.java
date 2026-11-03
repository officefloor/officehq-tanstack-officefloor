package net.officefloor.hq.app.rate;

import java.util.ArrayList;
import java.util.List;
import net.officefloor.hq.app.BadRequestException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Bind the rate import box: each non-blank line is grade,rate,from,to (the from and to may be absent or
 *  blank), checked by the rate form's rules. An import with no lines, or a line with more fields, is refused. */
public class ValidateRateImport {
    public void validate(@RequestBody RateImportRequest request, Out<RateImportInput> rates) throws BadRequestException {
        String text = request == null || request.text() == null ? "" : request.text();
        List<RateInput> checked = new ArrayList<>();
        int n = 0;
        for (String raw : text.split("\r?\n", -1)) {
            n++;
            if (raw.isBlank()) {
                continue;
            }
            String[] f = raw.split(",", -1);
            try {
                if (f.length > 4) {
                    throw new BadRequestException("a line is grade,rate,from,to");
                }
                checked.add(ValidateRate.check(new RateRequest(f[0], f.length > 1 ? f[1] : "",
                        f.length > 2 ? f[2] : "", f.length > 3 ? f[3] : "")));
            } catch (BadRequestException e) {
                throw new BadRequestException("line " + n + ": " + e.getMessage());
            }
        }
        if (checked.isEmpty()) {
            throw new BadRequestException("paste at least one rate");
        }
        rates.set(new RateImportInput(checked));
    }
}
