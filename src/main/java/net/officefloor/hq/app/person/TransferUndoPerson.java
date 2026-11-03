package net.officefloor.hq.app.person;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.CurrentUser;
import net.officefloor.hq.app.claim.ClaimRepository;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.settings.SettingsRepository;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.server.http.ServerHttpConnection;
import org.springframework.web.bind.annotation.PathVariable;

import java.time.LocalDate;

/** On the sheet a transfer brought a person to, a steward undoes it on the same sheet-today they arrived: the person
 *  is transferred back to the sheet they came from by exactly the transfer rules and lines (see {@link TransferPerson}),
 *  then this sheet writes PERSON_TRANSFER_UNDONE and its rollup lines against its figures before the undo. Refused for
 *  anyone but a steward, on any later day, and for a person who did not arrive by transfer. */
public class TransferUndoPerson {
    public void undo(@PathVariable("sheetId") String sheetId, @PathVariable("personId") String personId,
            ServerHttpConnection connection, PersonRepository people, AliasRepository aliases, ClaimRepository claims,
            SettingsRepository settings, SheetRepository sheets, Audit audit, Rollup rollup, Out<Person> person)
            throws BadRequestException {
        String by = CurrentUser.require(connection);
        String id = sheetId == null ? "" : sheetId.trim();
        long pid;
        try {
            pid = Long.parseLong(personId == null ? "" : personId.trim());
        } catch (NumberFormatException e) {
            throw new BadRequestException("no such person");
        }
        Person from = people.find(id, pid);
        if (from == null) {
            throw new BadRequestException("no such person");
        }
        if (!settings.role(id, by).equals(SettingsRepository.STEWARD)) {
            throw new BadRequestException("only a steward can undo a transfer");
        }
        LocalDate arrived = people.arrivedOn(id, pid);
        if (from.origin() == null || arrived == null) {
            throw new BadRequestException(from.name() + " did not arrive by transfer");
        }
        if (!arrived.equals(sheets.today(id))) {
            throw new BadRequestException("a transfer can only be undone on the day " + from.name() + " arrived");
        }
        String to = from.origin();
        Rollup.Snapshot before = rollup.take(id);
        long back = TransferPerson.move(id, pid, from, to, by, people, aliases, claims, settings, sheets, audit, rollup);
        audit.record("PERSON_TRANSFER_UNDONE sheet=" + id + " by=" + by + " name=" + from.name() + " to=" + to);
        rollup.record(id, by, before);
        person.set(people.find(to, back));
    }
}
