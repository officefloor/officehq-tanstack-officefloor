package net.officefloor.hq.app.paste;

import java.util.ArrayList;
import java.util.List;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.AuditUser;
import net.officefloor.hq.app.claim.Claim;
import net.officefloor.hq.app.claim.ClaimAudit;
import net.officefloor.hq.app.claim.ClaimLogRepository;
import net.officefloor.hq.app.claim.ClaimRules;
import net.officefloor.hq.app.claim.ClaimRepository;
import net.officefloor.hq.app.claim.InvalidClaimException;
import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonGradeRepository;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.rate.RateRepository;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestHeader;

/**
 * Finds each line's person on the sheet, ignoring case, and makes its DRAFT claim by the claim form's rules —
 * all of them before any is stored, so a refused line adds nothing. Then adds the claims in order, each recorded
 * and logged as the claim form records and logs it.
 */
public class AddPastedClaims {
    public void add(@Val PastedLines pasted, @Val Sheet sheet, PersonRepository people, ClaimRepository claims,
            PeriodRepository periods, PersonGradeRepository grades, RateRepository rates, ClaimLogRepository log,
            @RequestHeader(name = "X-User", required = false) String user, Audit audit,
            Out<PastedClaims> addedOut) throws InvalidClaimException {
        List<Person> onSheet = people.findBySheetIdOrderByIdAsc(sheet.getId());
        List<Person> persons = new ArrayList<>();
        List<Claim> made = new ArrayList<>();
        int at = 0;
        for (PastedLine line : pasted.lines()) {
            at++;
            Person person = onSheet.stream().filter(p -> p.getName().equalsIgnoreCase(line.name())).findFirst()
                    .orElse(null);
            if (person == null) {
                throw new InvalidClaimException("Line " + at + ": " + line.name() + " is not on the sheet.");
            }
            try {
                made.add(ClaimRules.make(line.claim(), sheet, person, claims, periods, grades));
            } catch (InvalidClaimException e) {
                throw new InvalidClaimException("Line " + at + ": " + e.getMessage());
            }
            persons.add(person);
        }
        String by = AuditUser.decode(user);
        for (int i = 0; i < made.size(); i++) {
            Claim claim = claims.saveAndFlush(made.get(i));
            audit.record(ClaimAudit.line("CLAIM_ADDED", claim, persons.get(i).getName(), by));
            ClaimRules.log(claim, sheet, log, people, rates, grades, by);
        }
        addedOut.set(new PastedClaims(made.size()));
    }
}
