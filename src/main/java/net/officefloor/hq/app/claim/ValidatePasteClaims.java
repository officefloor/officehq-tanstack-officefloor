package net.officefloor.hq.app.claim;

import java.util.ArrayList;
import java.util.List;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Reads the body: each non-blank line is name,amount,date,note (the note may hold commas and may be absent; a blank
 *  date takes the sheet's entry date, else its today), read by the claim form's rules. A note (its surrounding spaces removed) ending
 *  in ` #<reference>` gives the claim that reference, and that part is not part of the note. A line with no name, or a paste with no
 *  lines, is refused. */
public class ValidatePasteClaims {
    public void validate(@RequestBody PasteClaimsRequest request, Out<PastedLines> pasted) throws InvalidClaimException {
        String text = request == null || request.text() == null ? "" : request.text();
        String by = request == null || request.by() == null ? "" : request.by().trim();
        List<PastedLine> lines = new ArrayList<>();
        for (String line : text.split("\\r?\\n")) {
            if (line.isBlank()) {
                continue;
            }
            String[] fields = line.split(",", 4);
            String name = fields[0].trim();
            if (name.isEmpty()) {
                throw new InvalidClaimException("Each line must name a person");
            }
            String note = fields.length > 3 ? fields[3].trim() : "";
            String ref = "";
            int hash = note.lastIndexOf(" #");
            if (hash >= 0 && !note.substring(hash + 2).isBlank()) {
                ref = note.substring(hash + 2);
                note = note.substring(0, hash);
            }
            ClaimRequest claim = new ClaimRequest(fields.length > 1 ? fields[1] : "", fields.length > 2 ? fields[2] : "",
                    note, "", by, ref, null);
            lines.add(new PastedLine(name, ClaimValues.from(claim)));
        }
        if (lines.isEmpty()) {
            throw new InvalidClaimException("Paste at least one line of name,amount,date,note");
        }
        pasted.set(new PastedLines(lines, by));
    }
}
