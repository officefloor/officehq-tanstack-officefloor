package net.officefloor.hq.app.paste;

import jakarta.validation.Valid;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import net.officefloor.hq.app.claim.ClaimInput;
import net.officefloor.hq.app.claim.ClaimRequest;
import net.officefloor.hq.app.claim.InvalidClaimException;
import net.officefloor.hq.app.claim.ValidateClaimRequest;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Splits the text into lines of name,amount,date,note (the note may hold commas; the date may be blank; the note
 * may be absent; blank lines are skipped) and checks each with exactly the claim form's rules. Nothing pasted is
 * refused, as is a line without a name or amount. A pasted claim corrects nothing, so a negative amount is refused.
 * A note ending " #<reference>" gives the claim that external reference, which is not part of the note; a reference
 * an earlier line gave, ignoring case, is refused.
 */
public class ValidatePasteRequest {
    public void validate(@RequestBody @Valid PasteRequest request, Out<PastedLines> pasted) throws InvalidClaimException {
        String text = request.text() == null ? "" : request.text();
        List<PastedLine> lines = new ArrayList<>();
        Set<String> refs = new HashSet<>();
        int number = 0;
        for (String line : text.split("\\r?\\n")) {
            number++;
            if (line.isBlank()) {
                continue;
            }
            String[] fields = line.split(",", 4);
            String name = fields[0].strip();
            if (name.isEmpty()) {
                throw new InvalidClaimException("Line " + number + ": a name is required.");
            }
            if (fields.length < 2) {
                throw new InvalidClaimException("Line " + number + ": an amount is required.");
            }
            // The claim form's field limits, which its request checks on binding.
            if (fields[1].length() > 30 || (fields.length > 2 && fields[2].length() > 30)
                    || (fields.length > 3 && fields[3].length() > 1000)) {
                throw new InvalidClaimException("Line " + number + ": a field is too long.");
            }
            String note = fields.length > 3 ? fields[3].strip() : "";
            String ref = "";
            int hash = note.lastIndexOf(" #");
            if (hash >= 0 && !note.substring(hash + 2).isBlank()) {
                ref = note.substring(hash + 2).strip();
                note = note.substring(0, hash);
            }
            if (ref.length() > 120) {
                throw new InvalidClaimException("Line " + number + ": a field is too long.");
            }
            ClaimRequest claim = new ClaimRequest(fields[1], fields.length > 2 ? fields[2] : "", note, "", ref);
            ClaimInput[] input = new ClaimInput[1];
            try {
                new ValidateClaimRequest().validate(claim, value -> input[0] = value);
            } catch (InvalidClaimException e) {
                throw new InvalidClaimException("Line " + number + ": " + e.getMessage());
            }
            if (input[0].ref() != null && !refs.add(input[0].ref().toLowerCase(Locale.ROOT))) {
                throw new InvalidClaimException("Line " + number + ": the reference " + input[0].ref()
                        + " is already given by an earlier line.");
            }
            lines.add(new PastedLine(name, input[0], null));
        }
        if (lines.isEmpty()) {
            throw new InvalidClaimException("Paste at least one line of name,amount,date,note.");
        }
        pasted.set(new PastedLines(lines));
    }
}
