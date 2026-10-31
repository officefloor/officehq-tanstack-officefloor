package net.officefloor.hq.app.claim;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.officefloor.hq.app.rollup.Actor;
import net.officefloor.hq.app.sheet.InvalidSheetRequestException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Splits the paste into lines of name,amount,date,note (the note may hold commas, the date may be
 *  blank, the note absent) and checks each by the claim form's rules; blank lines are skipped. A
 *  note ending in ` #<reference>` gives the claim that reference (not part of the note); a reference
 *  repeated within the paste, without regard to case, refuses it. */
public class ValidatePasteClaims {
    /** A note ending in a space, a hash and the reference. */
    private static final Pattern REF = Pattern.compile("(.*) #(.+)");

    public void validate(@RequestBody PasteClaimsRequest request, Out<PastedClaims> body, Out<Actor> actor)
            throws InvalidSheetRequestException {
        String user = request == null ? null : request.user();
        String text = request == null || request.text() == null ? "" : request.text();
        List<PastedLine> lines = new ArrayList<>();
        Set<String> refs = new HashSet<>();
        int n = 0;
        for (String raw : text.split("\\r?\\n")) {
            n++;
            if (raw.isBlank()) continue;
            String[] f = raw.split(",", 4);
            if (f.length < 2 || f[0].isBlank()) {
                throw new InvalidSheetRequestException("Line " + n + ": a name and an amount are needed");
            }
            String note = f.length > 3 ? f[3] : "";
            String ref = null;
            Matcher tagged = REF.matcher(note.trim());
            if (tagged.matches() && !tagged.group(2).isBlank()) {
                note = tagged.group(1);
                ref = tagged.group(2);
            }
            ClaimValues values;
            try {
                values = ClaimRequest.checked(new ClaimRequest(f[1], f.length > 2 ? f[2] : "", note, null, user, ref));
            } catch (InvalidSheetRequestException e) {
                throw new InvalidSheetRequestException("Line " + n + ": " + e.getMessage());
            }
            if (values.ref() != null && !refs.add(values.ref().toLowerCase(Locale.ROOT))) {
                throw new InvalidSheetRequestException("Line " + n + ": the reference is repeated in the paste: " + values.ref());
            }
            lines.add(new PastedLine(f[0].trim(), values));
        }
        if (lines.isEmpty()) throw new InvalidSheetRequestException("Paste at least one line of name,amount,date,note");
        actor.set(new Actor(user));
        body.set(new PastedClaims(lines, user));
    }
}
