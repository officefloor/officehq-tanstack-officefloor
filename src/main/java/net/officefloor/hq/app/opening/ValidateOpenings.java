package net.officefloor.hq.app.opening;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import net.officefloor.hq.app.BadRequestException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Bind the opening balances paste box: each non-blank line is name,period,amount, the name and period not
 *  blank and the amount two decimals, 0.00 or more. A paste with no lines, or a line with other fields, is refused. */
public class ValidateOpenings {
    public void validate(@RequestBody OpeningRequest request, Out<OpeningInput> openings) throws BadRequestException {
        String text = request == null || request.text() == null ? "" : request.text();
        List<OpeningLine> checked = new ArrayList<>();
        int n = 0;
        for (String raw : text.split("\r?\n", -1)) {
            n++;
            if (raw.isBlank()) {
                continue;
            }
            String[] f = raw.split(",", -1);
            if (f.length != 3) {
                throw new BadRequestException("line " + n + ": a line is name,period,amount");
            }
            String name = f[0].trim();
            String period = f[1].trim();
            String amount = f[2].trim();
            if (name.isEmpty()) {
                throw new BadRequestException("line " + n + ": name is required");
            }
            if (period.isEmpty()) {
                throw new BadRequestException("line " + n + ": period is required");
            }
            if (!amount.matches("\\d+\\.\\d{2}")) {
                throw new BadRequestException("line " + n + ": amount must be 0.00 or more, with two decimals");
            }
            checked.add(new OpeningLine(n, name, period, new BigDecimal(amount)));
        }
        if (checked.isEmpty()) {
            throw new BadRequestException("paste at least one opening balance");
        }
        openings.set(new OpeningInput(checked));
    }
}
