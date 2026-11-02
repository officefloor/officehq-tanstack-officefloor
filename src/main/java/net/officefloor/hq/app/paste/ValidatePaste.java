package net.officefloor.hq.app.paste;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.claim.ClaimInput;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Bind the paste box: each non-blank line is name,amount,date,note — the name is required (trimmed), the
 *  amount and date follow the claim form's rules (a blank date: the sheet's today), and the note is the rest
 *  of the line (it may hold commas, or be absent). A paste with no lines is refused. */
public class ValidatePaste {
    public void validate(@RequestBody PasteRequest request, Out<PasteInput> paste) throws BadRequestException {
        String text = request == null || request.text() == null ? "" : request.text();
        List<PasteLine> lines = new ArrayList<>();
        int n = 0;
        for (String raw : text.split("\r?\n", -1)) {
            n++;
            if (raw.isBlank()) {
                continue;
            }
            String[] f = raw.split(",", 4);
            String name = f[0].trim();
            String amount = f.length > 1 ? f[1].trim() : "";
            String date = f.length > 2 ? f[2].trim() : "";
            String note = f.length > 3 ? f[3].trim() : "";
            if (name.isEmpty()) {
                throw new BadRequestException("line " + n + ": name is required");
            }
            LocalDate on = null;
            if (!date.isEmpty()) {
                try {
                    on = LocalDate.parse(date);
                } catch (DateTimeParseException e) {
                    throw new BadRequestException("line " + n + ": date must be yyyy-mm-dd");
                }
            }
            try {
                lines.add(new PasteLine(name, new ClaimInput(ClaimInput.signedAmount(amount), on, note, null)));
            } catch (BadRequestException e) {
                throw new BadRequestException("line " + n + ": " + e.getMessage());
            }
        }
        if (lines.isEmpty()) {
            throw new BadRequestException("paste at least one line");
        }
        paste.set(new PasteInput(lines));
    }
}
