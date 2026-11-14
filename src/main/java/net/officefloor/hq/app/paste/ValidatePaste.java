package net.officefloor.hq.app.paste;

import java.util.ArrayList;
import java.util.List;
import net.officefloor.hq.app.claim.ClaimRequest;
import net.officefloor.hq.app.claim.InvalidClaimException;
import net.officefloor.hq.app.claim.ValidClaim;
import net.officefloor.hq.app.claim.ClaimForm;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Splits the paste into its lines (blank lines skipped) and checks each with the claim form's own
 *  rules; a paste with no lines, or a line without a name, is refused. */
public class ValidatePaste {
    public void validate(@RequestBody PasteRequest request, Out<PasteLines> lines)
            throws InvalidClaimException {
        String text = request == null || request.text() == null ? "" : request.text();
        List<PasteLine> checked = new ArrayList<>();
        for (String line : text.split("\\r?\\n")) {
            if (line.isBlank()) continue;
            String[] f = line.split(",", 4);
            String name = f[0].trim();
            if (name.isEmpty()) {
                throw new InvalidClaimException("Each line needs a name: " + line);
            }
            ValidClaim claim = ClaimForm.check(new ClaimRequest(f.length > 1 ? f[1] : "",
                    f.length > 2 ? f[2] : "", f.length > 3 ? f[3] : ""));
            checked.add(new PasteLine(name, claim));
        }
        if (checked.isEmpty()) {
            throw new InvalidClaimException("Paste at least one line of name,amount,date,note");
        }
        lines.set(new PasteLines(checked));
    }
}
