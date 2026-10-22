package net.officefloor.hq.app.claim;

import java.util.ArrayList;
import java.util.List;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Bind the body and refuse it before anything is read or written. Each non-blank line is
 *  name,amount,date,note — the date may be blank (the sheet's today), the note may be absent and may itself
 *  hold commas — and must pass exactly the claim form's rules; a name is required. A paste with no lines is
 *  refused. A refusal names the first line that fails. */
public class ValidatePaste {
    public void validate(@RequestBody PasteRequest request, Out<PasteInput> body) throws InvalidClaimException {
        if (request == null || request.user() == null || request.user().isBlank()) {
            throw new InvalidClaimException("Sign in before changing claims.");
        }
        String user = request.user().trim();
        String[] rows = (request.text() == null ? "" : request.text()).split("\\r?\\n|\\r", -1);
        List<PasteLine> lines = new ArrayList<>();
        for (int i = 0; i < rows.length; i++) {
            if (rows[i].isBlank()) {
                continue;
            }
            int number = i + 1;
            String[] fields = rows[i].split(",", 4);
            String name = fields[0].trim();
            if (name.isEmpty()) {
                throw new InvalidClaimException("Line " + number + ": a name is required.");
            }
            try {
                ClaimInput claim = ValidateClaim.check(new ClaimRequest(user, fields.length > 1 ? fields[1] : "",
                        fields.length > 2 ? fields[2] : "", fields.length > 3 ? fields[3] : "", ""));
                lines.add(new PasteLine(number, name, claim));
            } catch (InvalidClaimException e) {
                throw new InvalidClaimException("Line " + number + ": " + e.getMessage());
            }
        }
        if (lines.isEmpty()) {
            throw new InvalidClaimException("Paste at least one line: name,amount,date,note.");
        }
        body.set(new PasteInput(user, lines));
    }
}
