package net.officefloor.hq.app.person;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.claim.Claim;
import net.officefloor.hq.app.claim.Claims;
import net.officefloor.hq.app.period.Period;
import net.officefloor.hq.app.period.Periods;
import net.officefloor.hq.app.rollup.Figures;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.setting.Settings;
import net.officefloor.hq.app.sheet.Sheets;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import net.officefloor.server.http.ServerHttpConnection;
import org.springframework.web.bind.annotation.RequestBody;

public class TransferPerson {
    // Only a steward (a delegation included) transfers a person who is not retired, to another sheet that is not archived
    // and has none of their name, aliases or contact. Each numbered claim takes the next number of the other sheet, in
    // the order the claims were opened (the sheet's today when each was first opened, then the order they were added).
    public void transfer(@RequestBody(required = false) TransferRequest body, @Val Person person, People people,
            Claims claims, Settings settings, Sheets sheets, Periods periods, Rollup rollup,
            ServerHttpConnection connection, Out<TransferredPerson> transferredOut, Out<PersonResponse> responseOut)
            throws PersonTransferRefusedException {
        if (!Settings.STEWARD.equals(settings.role(person.sheetId(), Audit.user(connection).trim()))) {
            throw new PersonTransferRefusedException("Only a steward can transfer a person.");
        }
        String to = body == null || body.toSheetId() == null ? "" : body.toSheetId().trim();
        if (to.isEmpty()) {
            throw new PersonTransferRefusedException("Give the id of the sheet to transfer to.");
        }
        if (to.equals(person.sheetId())) {
            throw new PersonTransferRefusedException("A person cannot be transferred to their own sheet.");
        }
        if (person.retired()) {
            throw new PersonTransferRefusedException("A retired person cannot be transferred.");
        }
        if (sheets.isArchived(to)) {
            throw new PersonTransferRefusedException("The sheet " + to + " is archived.");
        }
        if (people.nameOrAliasTaken(to, person.name())
                || person.aliases().stream().anyMatch(a -> people.nameOrAliasTaken(to, a))) {
            throw new PersonTransferRefusedException("A name or alias of theirs is already on the sheet " + to + ".");
        }
        if (person.contact() != null && people.contactTaken(to, person.contact(), -1)) {
            throw new PersonTransferRefusedException("Their contact is already on the sheet " + to + ".");
        }
        Figures before = rollup.figures(to);

        // The numbered claims in the order they were opened, each given the next number of the other sheet.
        Map<Long, LocalDate> opened = claims.openedOn(person.sheetId());
        List<Claim> numbered = new ArrayList<>(claims.list(person.sheetId()).stream()
                .filter(c -> c.personId() == person.id() && c.number() != null).toList());
        numbered.sort(Comparator.<Claim, LocalDate>comparing(c -> opened.getOrDefault(c.id(), c.date()))
                .thenComparingLong(Claim::id));
        LocalDate numberingFrom = settings.periodNumberingFrom(to);
        boolean byPeriod = numberingFrom != null && !sheets.today(to).isBefore(numberingFrom);
        Map<Long, String> numbers = new LinkedHashMap<>();
        for (Claim claim : numbered) {
            String scope = null;
            if (byPeriod) {
                Period period = periods.on(to, claim.dueBy());
                scope = period == null ? "" : period.name();
            }
            numbers.put(claim.id(), claims.number(to, scope));
        }

        long arrived = people.transfer(person, to, numbers);
        sheets.markLoaded(to);
        transferredOut.set(new TransferredPerson(person, to, before));
        responseOut.set(PersonResponse.from(people.find(to, arrived), sheets.today(to)));
    }
}
