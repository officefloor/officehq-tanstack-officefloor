package net.officefloor.hq.app.person;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.officefloor.hq.app.claim.ClaimNumbers;
import net.officefloor.hq.app.claim.Claim;
import net.officefloor.hq.app.claim.ClaimRepository;
import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.hq.app.setting.SettingRepository;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/**
 * Transfers the person to the other sheet: they arrive as its newest person with their grades, terms, contact and
 * aliases (statements, holds, patron links, pool memberships and watches dropped), and their claims arrive in the
 * order they were added, keeping status, dates, notes, frozen figures, tags and logs, losing any figure a statement
 * bound and their references. Each claim that had a number takes the other sheet's next one, in the order the claims
 * were opened; a correction or split that named one of them by number names its new number. They leave this sheet.
 */
public class TransferPerson {
    public void transfer(@Val Transfer transfer, PersonRepository people, ClaimRepository claims,
            SheetRepository sheets, SettingRepository settings, PeriodRepository periods, Out<Transferred> arrived) {
        Person person = transfer.person();
        String to = transfer.to();
        long personId = people.transfer(person, to);
        List<ClaimRepository.Moved> moved = claims.transfer(person.sheetId(), person.id(), to, personId);
        Map<String, String> numbers = new HashMap<>();
        for (ClaimRepository.Moved m : moved.stream().filter((m) -> m.number() != null)
                .sorted(Comparator.comparing(ClaimRepository.Moved::opened)
                        .thenComparing(ClaimRepository.Moved::id))
                .toList()) {
            Claim claim = claims.find(to, m.id());
            numbers.put(m.number(), ClaimNumbers.next(claim, sheets, settings, periods));
        }
        claims.renumberTransferred(to, moved, numbers);
        people.dropTransferred(person);
        arrived.set(new Transferred(people.find(to, personId)));
    }
}
