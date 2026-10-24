package net.officefloor.hq.app.paste;

import java.util.ArrayList;
import java.util.List;
import net.officefloor.hq.app.claim.ClaimInput;
import net.officefloor.hq.app.claim.InvalidClaimException;
import net.officefloor.hq.app.claim.ClaimRules;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Reads the paste box from the body: each non-blank line is name,amount,date,note — the note is the rest of the
 * line, so it may hold commas; the date and the note may be blank or absent. Checks every line's claim exactly
 * as the claim form would (it corrects nothing), refusing the whole paste when any line fails or there is none.
 */
public class ValidatePaste {
    public void validate(@RequestBody PasteRequest request, Out<PastedLines> linesOut) throws InvalidClaimException {
        String text = request == null || request.text() == null ? "" : request.text();
        List<PastedLine> lines = new ArrayList<>();
        int at = 0;
        for (String line : text.split("\\r?\\n")) {
            at++;
            if (line.isBlank()) {
                continue;
            }
            String[] fields = line.split(",", 4);
            String name = fields[0].trim();
            if (name.isEmpty() || fields.length < 2) {
                throw new InvalidClaimException("Line " + at + ": a line is name,amount,date,note.");
            }
            ClaimInput claim;
            try {
                claim = ClaimRules.check(fields[1], fields.length > 2 ? fields[2] : null,
                        fields.length > 3 ? fields[3] : null, null);
            } catch (InvalidClaimException e) {
                throw new InvalidClaimException("Line " + at + ": " + e.getMessage());
            }
            lines.add(new PastedLine(name, claim));
        }
        if (lines.isEmpty()) {
            throw new InvalidClaimException("Paste at least one line of name,amount,date,note.");
        }
        linesOut.set(new PastedLines(List.copyOf(lines)));
    }
}
