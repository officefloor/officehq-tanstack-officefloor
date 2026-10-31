package net.officefloor.hq.app.claim;

import java.util.ArrayList;
import java.util.List;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.holiday.Holidays;
import net.officefloor.hq.app.period.ClosedPeriods;
import net.officefloor.hq.app.period.PeriodClosedException;
import net.officefloor.hq.app.person.People;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRetiredException;
import net.officefloor.hq.app.settings.Settings;
import net.officefloor.hq.app.sheet.InvalidSheetRequestException;
import net.officefloor.hq.app.sheet.Sheets;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

/** Builds every pasted claim first, as the claim form would for its named person, so one refused
 *  line refuses the whole paste before anything is saved; then saves and audits each in order. */
public class AddPastedClaims {
    public void add(@PathVariable("sheetId") String sheetId, @Val PastedClaims paste, People people, Sheets sheets,
            ClosedPeriods closedPeriods, Claims claims, Settings settings, Holidays holidays, ClaimLog log, Audit audit,
            Out<PasteClaimsResult> result) throws PeriodClosedException, PersonRetiredException, InvalidSheetRequestException {
        List<Person> persons = new ArrayList<>();
        List<Claim> built = new ArrayList<>();
        for (PastedLine line : paste.lines()) {
            Person person = people.named(sheetId, line.name());
            if (person == null) throw new InvalidSheetRequestException("No one on the sheet is named " + line.name());
            persons.add(person);
            built.add(BuildClaim.built(line.values(), person, sheets, closedPeriods, claims, settings, holidays));
        }
        RecordClaimAdded record = new RecordClaimAdded();
        for (int i = 0; i < built.size(); i++) {
            claims.save(built.get(i));
            record.record(paste.lines().get(i).values(), persons.get(i), built.get(i), log, audit);
        }
        result.set(new PasteClaimsResult(built.size()));
    }
}
