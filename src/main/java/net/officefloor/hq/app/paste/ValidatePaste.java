package net.officefloor.hq.app.paste;

import java.util.ArrayList;
import java.util.List;
import net.officefloor.hq.app.claim.ClaimAdding;
import net.officefloor.hq.app.claim.ClaimInput;
import net.officefloor.hq.app.claim.ClaimRequest;
import net.officefloor.hq.app.claim.ClaimStore;
import net.officefloor.hq.app.period.PeriodStore;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonStore;
import net.officefloor.hq.app.sheet.InvalidSheetException;
import net.officefloor.hq.app.sheet.SheetStore;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Checks every pasted line with exactly the claim form's rules before anything is stored: the name
 * must be on the sheet (ignoring case and the spaces around it), the amount a number with up to two
 * decimals (a negative one names no claim it corrects, so is refused), the date (blank for the
 * sheet's today) in no closed period and not after the person is retired from. The note is the rest
 * of the line, commas and all. Blank lines are skipped; a paste with no line is refused.
 */
public class ValidatePaste {
    public void validate(@PathVariable("sheetId") String sheetId, @RequestBody PasteRequest request,
            SheetStore sheets, PersonStore people, PeriodStore periods, ClaimStore claims, Out<PasteInput> pasted)
            throws InvalidSheetException {
        String sheet = sheetId.trim();
        String text = request == null || request.text() == null ? "" : request.text();
        List<PastedClaim> pastedClaims = new ArrayList<>();
        String[] lines = text.split("\\r?\\n");
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            if (line.trim().isEmpty()) {
                continue;
            }
            String[] fields = line.split(",", 4);
            if (fields.length < 2) {
                throw new InvalidSheetException("Line " + (i + 1) + " must be name,amount,date,note");
            }
            try {
                Person person = people.findByName(sheet, fields[0]);
                if (person == null) {
                    throw new InvalidSheetException("No such person: " + fields[0].trim());
                }
                ClaimInput claim = ClaimAdding.check(sheet, person, new ClaimRequest(fields[1],
                        fields.length > 2 ? fields[2] : "", fields.length > 3 ? fields[3] : "", request.user(), ""),
                        sheets, periods, claims);
                pastedClaims.add(new PastedClaim(person, claim));
            } catch (InvalidSheetException e) {
                throw new InvalidSheetException("Line " + (i + 1) + ": " + e.getMessage());
            }
        }
        if (pastedClaims.isEmpty()) {
            throw new InvalidSheetException("Paste at least one line of name,amount,date,note");
        }
        pasted.set(new PasteInput(pastedClaims));
    }
}
