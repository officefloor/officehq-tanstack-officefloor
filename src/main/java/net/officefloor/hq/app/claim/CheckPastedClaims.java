package net.officefloor.hq.app.claim;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.officefloor.hq.app.holiday.HolidayRepository;
import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.role.RoleRepository;
import net.officefloor.hq.app.setting.SettingRepository;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

/** Checks every pasted line before any is added: the user pasting must be one the sheet permits to open (see
 *  SettingRepository#permits), its person must be on the sheet (names compared as names are, case and outer spaces
 *  ignored) and, unless the user is a steward, not flagged on the sheet's today (see Person#flaggedOn), its reference not repeating one on an earlier line (case ignored). A line whose person, date and note
 *  match claims of that person as they stood before the paste (not VOID nor DELETED) updates the newest such DRAFT
 *  (one an earlier line of the paste has not taken) to the line's amount, which must keep the rules of editing a claim (see ApplyClaim);
 *  a line whose matches include no such DRAFT fails. Any other line must keep the rules of adding a claim (see
 *  ClaimRules). Any failing line refuses the whole paste. */
public class CheckPastedClaims {
    public void check(@PathVariable("sheetId") String sheetId, @Val PastedLines pasted, PersonRepository people,
            SettingRepository settings, PeriodRepository periods, ClaimRepository claims, HolidayRepository holidays,
            RoleRepository roles, SheetRepository sheets, Out<PastedClaims> checked)
            throws InvalidClaimException {
        String sheet = sheetId.trim();
        String role = roles.roleOf(sheet, pasted.by());
        if (!settings.permits(sheet, "open", role)) {
            throw new InvalidClaimException("A " + role + " may not open a claim");
        }
        LocalDate today = sheets.today(sheet);
        List<Claim> stood = claims.list(sheet);
        Set<Long> taken = new HashSet<>();
        List<PastedClaim> all = new ArrayList<>();
        Set<String> refs = new HashSet<>();
        for (PastedLine line : pasted.lines()) {
            Person person = people.findByName(sheet, line.name());
            if (person == null) {
                throw new InvalidClaimException("No person named " + line.name() + " on the sheet");
            }
            if (!RoleRepository.STEWARD.equals(role) && person.flaggedOn(today)) {
                throw new InvalidClaimException("Only a steward may open a claim of " + person.name() + ", who is flagged");
            }
            String ref = line.values().ref();
            if (ref != null && !refs.add(ClaimValues.refKey(ref))) {
                throw new InvalidClaimException("The reference " + ref + " is pasted more than once");
            }
            LocalDate date = line.values().date() != null ? line.values().date() : settings.claimDate(sheet);
            String note = noteOf(line.values().note());
            List<Claim> matches = stood.stream()
                    .filter((c) -> c.personId() == person.id() && date.equals(c.date())
                            && note.equals(noteOf(c.note())) && c.status() != ClaimStatus.VOID
                            && c.status() != ClaimStatus.DELETED)
                    .toList();
            if (matches.isEmpty()) {
                all.add(new PastedClaim(person, line.values(),
                        ClaimRules.check(person, line.values(), settings, periods, claims, holidays), null));
                continue;
            }
            Claim draft = matches.stream()
                    .filter((c) -> c.status() == ClaimStatus.DRAFT && !taken.contains(c.id()))
                    .max(Comparator.comparingLong(Claim::id)).orElse(null);
            if (draft == null) {
                throw new InvalidClaimException("The line for " + line.name() + " matches no DRAFT claim to update");
            }
            taken.add(draft.id());
            if (periods.closedOn(sheet, draft.dueBy())) {
                throw new InvalidClaimException("The claim's due-by is inside a closed period");
            }
            if (!person.mayClaimOn(date)) {
                throw new InvalidClaimException("The claim's date is after the person's retirement date");
            }
            if (line.values().amount().compareTo(draft.amount()) != 0 && claims.hasLines(sheet, draft.id())) {
                throw new InvalidClaimException("A claim with lines takes its amount from them");
            }
            all.add(new PastedClaim(person, line.values(),
                    new ClaimRules.Checked(date, draft.corrects(), draft.dueBy(), 0), draft));
        }
        checked.set(new PastedClaims(all));
    }

    private static String noteOf(String note) {
        return note == null ? "" : note.trim();
    }
}
