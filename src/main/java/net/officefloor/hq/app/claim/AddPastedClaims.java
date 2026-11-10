package net.officefloor.hq.app.claim;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.period.CheckClaimPeriod;
import net.officefloor.hq.app.period.ClosedPeriodException;
import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.hq.app.person.CheckClaimRetired;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.person.RetiredPersonException;
import net.officefloor.hq.app.rate.RateRepository;
import net.officefloor.hq.app.setting.Settings;
import net.officefloor.hq.app.setting.SettingsRepository;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import net.officefloor.hq.app.paste.PasteLine;
import net.officefloor.hq.app.paste.PasteLines;
import net.officefloor.hq.app.paste.PasteResponse;

/**
 * Adds a DRAFT claim per pasted line, in order, for the person on the sheet with that name (matched
 * as names are compared). Every line is checked first, as the claim form checks a claim (its person
 * on the sheet, its date outside a closed period and not after its person's retirement), so a
 * refused paste writes nothing; then each claim is saved, logged and audited as one added by the form.
 */
public class AddPastedClaims {
    public void add(@Val Sheet sheet, @Val PasteLines paste, PersonRepository people, ClaimRepository claims,
            PeriodRepository periods, RateRepository rates, SettingsRepository settings, ClaimLogRepository log,
            HttpServletRequest request, Audit audit, Out<PasteResponse> result)
            throws InvalidClaimException, ClosedPeriodException, RetiredPersonException {
        Settings terms = settings.find(sheet.id());
        List<Person> persons = new ArrayList<>();
        List<Claim> built = new ArrayList<>();
        for (PasteLine line : paste.lines()) {
            Person person = people.findByName(sheet.id(), line.name());
            if (person == null) throw new InvalidClaimException("No one named " + line.name() + " is on the sheet");
            LocalDate date = line.claim().date() == null ? sheet.today() : line.claim().date();
            Claim claim = new Claim(null, sheet.id(), person.getId(), line.claim().amount(), date, line.claim().note(),
                    ClaimStatus.DRAFT, null, null, null);
            claim.setDueBy(date.plusDays(terms.termsFor(person, date)));
            new CheckClaimPeriod().check(sheet, claim, periods);
            claim.setLateEntry(periods.softClosedOn(sheet.id(), date) != null);
            new CheckClaimRetired().check(sheet, claim, people);
            persons.add(person);
            built.add(claim);
        }
        String user = Audit.user(request);
        for (int i = 0; i < built.size(); i++) {
            Claim claim = built.get(i);
            claims.save(claim);
            log.record(claim, user, sheet.today(), LogClaimStatus.due(sheet, claim, people, rates, settings));
            new AuditClaimAdded().audit(sheet, persons.get(i), claim, request, audit);
        }
        result.set(new PasteResponse(built.size()));
    }
}
