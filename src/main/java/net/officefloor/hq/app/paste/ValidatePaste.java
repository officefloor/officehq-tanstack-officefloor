package net.officefloor.hq.app.paste;

import java.util.ArrayList;
import java.util.List;
import net.officefloor.hq.app.claim.ClaimInput;
import net.officefloor.hq.app.claim.ClaimRequest;
import net.officefloor.hq.app.claim.InvalidClaimException;
import net.officefloor.hq.app.person.InvalidPersonException;
import net.officefloor.hq.app.person.PersonRequest;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Splits the paste into lines (blank lines skipped) and checks each by the claim form's rules; the
 * first line that fails refuses the whole paste. A paste with no lines is refused too.
 */
public class ValidatePaste {
    public void validate(@RequestBody PasteRequest request, Out<PasteLines> linesOut) throws InvalidClaimException {
        String text = request == null || request.text() == null ? "" : request.text();
        List<PasteLine> lines = new ArrayList<>();
        int number = 0;
        for (String raw : text.split("\\r?\\n|\\r")) {
            number++;
            if (raw.isBlank()) continue;
            String[] fields = raw.split(",", 4);
            if (fields.length < 2) throw new InvalidClaimException("Line " + number + ": name,amount,date,note");
            String name;
            try {
                name = PersonRequest.checkName(fields[0]);
            } catch (InvalidPersonException e) {
                throw new InvalidClaimException("Line " + number + ": " + e.getMessage());
            }
            String date = fields.length > 2 ? fields[2] : "";
            String note = fields.length > 3 ? fields[3] : "";
            try {
                ClaimInput claim = ClaimInput.from(new ClaimRequest(fields[1], date, note, ""));
                lines.add(new PasteLine(name, claim));
            } catch (InvalidClaimException e) {
                throw new InvalidClaimException("Line " + number + ": " + e.getMessage());
            }
        }
        if (lines.isEmpty()) throw new InvalidClaimException("Paste at least one line of name,amount,date,note");
        linesOut.set(new PasteLines(lines));
    }
}
