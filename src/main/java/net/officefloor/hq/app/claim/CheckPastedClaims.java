package net.officefloor.hq.app.claim;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.officefloor.hq.app.holiday.HolidayRepository;
import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.setting.SettingRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

/** Checks every pasted line before any is added: its person must be on the sheet (names compared as names are, case
 *  and outer spaces ignored) and its claim must keep the rules of adding a claim (see ClaimRules), its reference
 *  not repeating one on an earlier line (case ignored). Any failing line refuses the whole paste. */
public class CheckPastedClaims {
    public void check(@PathVariable("sheetId") String sheetId, @Val PastedLines pasted, PersonRepository people,
            SettingRepository settings, PeriodRepository periods, ClaimRepository claims, HolidayRepository holidays,
            Out<PastedClaims> checked)
            throws InvalidClaimException {
        String sheet = sheetId.trim();
        List<PastedClaim> all = new ArrayList<>();
        Set<String> refs = new HashSet<>();
        for (PastedLine line : pasted.lines()) {
            Person person = people.findByName(sheet, line.name());
            if (person == null) {
                throw new InvalidClaimException("No person named " + line.name() + " on the sheet");
            }
            String ref = line.values().ref();
            if (ref != null && !refs.add(ClaimValues.refKey(ref))) {
                throw new InvalidClaimException("The reference " + ref + " is pasted more than once");
            }
            all.add(new PastedClaim(person, line.values(), ClaimRules.check(person, line.values(), settings, periods, claims, holidays)));
        }
        checked.set(new PastedClaims(all));
    }
}
