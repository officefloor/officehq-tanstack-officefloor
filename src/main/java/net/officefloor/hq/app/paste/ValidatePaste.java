package net.officefloor.hq.app.paste;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import net.officefloor.hq.app.claim.Claim;
import net.officefloor.hq.app.claim.ClaimAdding;
import net.officefloor.hq.app.claim.ClaimInput;
import net.officefloor.hq.app.claim.ClaimRequest;
import net.officefloor.hq.app.claim.ClaimStatus;
import net.officefloor.hq.app.claim.ClaimStore;
import net.officefloor.hq.app.holiday.HolidayStore;
import net.officefloor.hq.app.period.PeriodStore;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonStore;
import net.officefloor.hq.app.role.RoleStore;
import net.officefloor.hq.app.sheet.InvalidSheetException;
import net.officefloor.hq.app.setting.SettingStore;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Checks every pasted line with exactly the claim form's rules before anything is stored: the name
 * (or an alias) must be on the sheet (ignoring case and the spaces around it), the amount a number with up to two
 * decimals (a negative one names no claim it corrects, so is refused), the date (blank for the
 * entry date while it is set, else the sheet's today) due by (with the terms it would get) a date in no closed period and not after the person is retired from. The note is the rest
 * of the line, commas and all, less a closing " #<reference>", which is the claim's external reference: one
 * already on the sheet or given by an earlier line, compared without regard to case, is refused. Blank lines are skipped; a paste with no line is refused.
 * A line naming a person, date and note that match one of their claims as they stood before the paste (void and deleted
 * ones aside) updates the newest such DRAFT instead of adding a claim; one whose matches include no DRAFT is refused.
 */
public class ValidatePaste {
    public void validate(@PathVariable("sheetId") String sheetId, @RequestBody PasteRequest request,
            SettingStore settings, PersonStore people, PeriodStore periods, ClaimStore claims, Out<PasteInput> pasted, RoleStore roles, HolidayStore holidays) throws InvalidSheetException {
        roles.refuseChange(sheetId.trim(), request == null ? null : request.user());
        String sheet = sheetId.trim();
        String text = request == null || request.text() == null ? "" : request.text();
        List<PastedClaim> pastedClaims = new ArrayList<>();
        String[] lines = text.split("\\r?\\n");
        Set<String> refs = new HashSet<>();
        // The claims as they stood before the paste, newest first: what a line may match.
        List<Claim> before = claims.list(sheet);
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
                String note = fields.length > 3 ? fields[3].trim() : "";
                String ref = "";
                int hash = note.lastIndexOf(" #");
                if (hash >= 0 && !note.substring(hash + 2).trim().isEmpty()) {
                    ref = note.substring(hash + 2).trim();
                    note = note.substring(0, hash);
                }
                ClaimInput claim = ClaimAdding.check(sheet, person, new ClaimRequest(fields[1],
                        fields.length > 2 ? fields[2] : "", note, request.user(), "", ref),
                        settings, periods, claims, holidays);
                if (claim.ref() != null && !refs.add(claim.ref().toLowerCase(Locale.ROOT))) {
                    throw new InvalidSheetException("The reference " + claim.ref() + " is given twice");
                }
                pastedClaims.add(new PastedClaim(person, claim, matchedDraft(before, person, claim)));
            } catch (InvalidSheetException e) {
                throw new InvalidSheetException("Line " + (i + 1) + ": " + e.getMessage());
            }
        }
        if (pastedClaims.isEmpty()) {
            throw new InvalidSheetException("Paste at least one line of name,amount,date,note");
        }
        pasted.set(new PasteInput(pastedClaims));
    }

    /** The newest DRAFT of the person's claims (void and deleted ones aside) with the line's date and
     *  note, null when none has them; refused when some have them but none is a DRAFT. */
    private static Long matchedDraft(List<Claim> before, Person person, ClaimInput line) throws InvalidSheetException {
        boolean matched = false;
        for (Claim c : before) {
            if (c.getPersonId() != person.getId() || c.getStatus() == ClaimStatus.VOID
                    || c.getStatus() == ClaimStatus.DELETED || !line.date().equals(c.getDate())
                    || !noteOf(line.note()).equals(noteOf(c.getNote()))) {
                continue;
            }
            if (c.getStatus() == ClaimStatus.DRAFT) {
                return c.getId();
            }
            matched = true;
        }
        if (matched) {
            throw new InvalidSheetException("It matches a claim of " + person.getName() + " that is no longer a DRAFT");
        }
        return null;
    }

    private static String noteOf(String note) {
        return note == null ? "" : note.trim();
    }
}
