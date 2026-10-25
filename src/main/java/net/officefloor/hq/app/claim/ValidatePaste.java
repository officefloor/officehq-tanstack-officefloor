package net.officefloor.hq.app.claim;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.session.ChangedBy;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * The paste box's lines of name,amount,date,note (the note may hold commas; the date may be blank for the sheet's
 * today; the note may be absent; blank lines are skipped). A note ending " #<reference>" gives the claim that
 * reference, not part of the note; a reference repeated within the paste refuses it. Each line's fields are checked with the claim form's
 * rules; a refusal names the line. Publishes them parsed, in order, with the user making the change.
 */
public class ValidatePaste {

    static final int MAX_LINES = 1000;

    public void validate(@RequestBody PasteRequest request, Out<PasteLines> pasted, Out<ChangedBy> changedBy)
            throws BadRequestException {
        ChangedBy by = ChangedBy.of(request == null ? null : request.by());
        String text = request == null || request.lines() == null ? "" : request.lines();
        List<PasteLine> lines = new ArrayList<>();
        Set<String> refs = new HashSet<>();
        String[] typed = text.split("\\r?\\n");
        for (int i = 0; i < typed.length; i++) {
            if (typed[i].trim().isEmpty()) {
                continue;
            }
            String[] fields = typed[i].split(",", 4);
            String name = fields[0].trim();
            if (name.isEmpty() || fields.length < 2) {
                throw new BadRequestException("Line " + (i + 1) + ": enter name,amount,date,note");
            }
            try {
                String note = fields.length > 3 ? fields[3].trim() : "";
                String ref = "";
                int hash = note.lastIndexOf(" #");
                if (hash >= 0 && !note.substring(hash + 2).trim().isEmpty()) {
                    ref = note.substring(hash + 2);
                    note = note.substring(0, hash);
                }
                ClaimInput input = ValidateClaim.parse(new ClaimRequest(fields[1],
                        fields.length > 2 ? fields[2] : "", note, null, ref, null, by.name()));
                if (input.ref() != null && !refs.add(input.ref().toLowerCase(Locale.ROOT))) {
                    throw RefuseUsedRef.used(input.ref());
                }
                lines.add(new PasteLine(name, input));
            } catch (BadRequestException e) {
                throw new BadRequestException("Line " + (i + 1) + ": " + e.getMessage());
            }
        }
        if (lines.isEmpty()) {
            throw new BadRequestException("Paste at least one line of name,amount,date,note");
        }
        if (lines.size() > MAX_LINES) {
            throw new BadRequestException("Paste at most " + MAX_LINES + " lines at once");
        }
        pasted.set(new PasteLines(lines));
        changedBy.set(by);
    }
}
