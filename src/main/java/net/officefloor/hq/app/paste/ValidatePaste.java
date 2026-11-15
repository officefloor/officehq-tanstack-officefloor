package net.officefloor.hq.app.paste;

import java.util.ArrayList;
import java.util.List;
import net.officefloor.hq.app.claim.ClaimRequest;
import net.officefloor.hq.app.claim.InvalidClaimException;
import net.officefloor.hq.app.claim.ValidClaim;
import net.officefloor.hq.app.claim.ClaimForm;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Splits the paste into its lines (blank lines skipped; a note ending in
 *  " #<reference>" gives the claim that reference) and checks each with the claim form's own
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
            // A note ending in " #<reference>" gives the claim's reference: that part is not the note.
            String note = f.length > 3 ? f[3].trim() : "";
            String ref = "";
            int hash = note.lastIndexOf(" #");
            if (hash >= 0 && !note.substring(hash + 2).isBlank()) {
                ref = note.substring(hash + 2);
                note = note.substring(0, hash);
            }
            ValidClaim claim = ClaimForm.check(new ClaimRequest(f.length > 1 ? f[1] : "",
                    f.length > 2 ? f[2] : "", note, null, ref));
            checked.add(new PasteLine(name, claim));
        }
        if (checked.isEmpty()) {
            throw new InvalidClaimException("Paste at least one line of name,amount,date,note");
        }
        lines.set(new PasteLines(checked));
    }
}
