package net.officefloor.hq.app.claim;

import java.util.ArrayList;
import java.util.List;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PathVariable;
import net.officefloor.hq.app.setting.Settings;
import net.officefloor.hq.app.setting.ViewerRefusedException;

/**
 * Splits the paste into lines of name,amount,date,note (the note may hold commas, the date may be
 * blank, the note absent) and checks each by the claim form's rules. Blank lines are skipped; a paste
 * with no claim in it is refused.
 */
public class ValidatePasteClaims {
    public void validate(@PathVariable("sheetId") String sheetId, @RequestBody PasteClaimsRequest request, Out<ValidPasteClaims> body, Settings settings)
            throws InvalidClaimException, ViewerRefusedException {
        settings.refuseViewer(sheetId, request.user());
        String text = request.text() == null ? "" : request.text();
        List<ValidPasteClaims.Line> lines = new ArrayList<>();
        int number = 0;
        for (String raw : text.split("\\r?\\n")) {
            number++;
            if (raw.isBlank()) continue;
            String[] fields = raw.split(",", 4);
            if (fields.length < 2) {
                throw new InvalidClaimException("Line " + number + ": a line is name,amount,date,note");
            }
            String name = fields[0].trim();
            if (name.isEmpty()) {
                throw new InvalidClaimException("Line " + number + ": a name is required");
            }
            // A note ending " #<reference>" gives the claim's reference: that part is not the note.
            String note = fields.length > 3 ? fields[3].trim() : "";
            String ref = null;
            int hash = note.lastIndexOf(" #");
            if (hash >= 0 && !note.substring(hash + 2).isBlank()) {
                ref = note.substring(hash + 2);
                note = note.substring(0, hash);
            }
            ClaimRequest claim = new ClaimRequest(fields[1], fields.length > 2 ? fields[2] : "", note, request.user(), null, ref, null);
            try {
                lines.add(new ValidPasteClaims.Line(name, ValidateClaim.check(claim)));
            } catch (InvalidClaimException e) {
                throw new InvalidClaimException("Line " + number + ": " + e.getMessage());
            }
        }
        java.util.Set<String> refs = new java.util.HashSet<>();
        for (ValidPasteClaims.Line line : lines) {
            String ref = line.claim().ref();
            if (ref != null && !refs.add(ref.toLowerCase(java.util.Locale.ROOT))) {
                throw new InvalidClaimException("The reference " + ref + " is given twice in the paste");
            }
        }
        if (lines.isEmpty()) {
            throw new InvalidClaimException("Nothing to paste: give one claim per line");
        }
        body.set(new ValidPasteClaims(lines, request.user()));
    }
}
