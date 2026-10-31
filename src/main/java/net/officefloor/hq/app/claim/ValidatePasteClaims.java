package net.officefloor.hq.app.claim;

import java.util.ArrayList;
import java.util.List;
import net.officefloor.hq.app.rollup.Actor;
import net.officefloor.hq.app.sheet.InvalidSheetRequestException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Splits the paste into lines of name,amount,date,note (the note may hold commas, the date may be
 *  blank, the note absent) and checks each by the claim form's rules; blank lines are skipped. */
public class ValidatePasteClaims {
    public void validate(@RequestBody PasteClaimsRequest request, Out<PastedClaims> body, Out<Actor> actor)
            throws InvalidSheetRequestException {
        String user = request == null ? null : request.user();
        String text = request == null || request.text() == null ? "" : request.text();
        List<PastedLine> lines = new ArrayList<>();
        int n = 0;
        for (String raw : text.split("\\r?\\n")) {
            n++;
            if (raw.isBlank()) continue;
            String[] f = raw.split(",", 4);
            if (f.length < 2 || f[0].isBlank()) {
                throw new InvalidSheetRequestException("Line " + n + ": a name and an amount are needed");
            }
            ClaimValues values;
            try {
                values = ClaimRequest.checked(new ClaimRequest(f[1], f.length > 2 ? f[2] : "", f.length > 3 ? f[3] : "", null, user));
            } catch (InvalidSheetRequestException e) {
                throw new InvalidSheetRequestException("Line " + n + ": " + e.getMessage());
            }
            lines.add(new PastedLine(f[0].trim(), values));
        }
        if (lines.isEmpty()) throw new InvalidSheetRequestException("Paste at least one line of name,amount,date,note");
        actor.set(new Actor(user));
        body.set(new PastedClaims(lines, user));
    }
}
