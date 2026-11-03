package net.officefloor.hq.app.person;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.CurrentUser;
import net.officefloor.hq.app.settings.SettingsRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import net.officefloor.server.http.ServerHttpConnection;
import org.springframework.web.bind.annotation.PathVariable;

/** A steward flags a person with a reason until a date (replacing any flag before it). While the sheet's today is on
 *  or before that date, only a steward may open a claim of the person; after it the flag has no effect. Refused for
 *  anyone but a steward. */
public class FlagPerson {
    public void flag(@PathVariable("sheetId") String sheetId, @PathVariable("personId") String personId,
            @Val FlagInput input, ServerHttpConnection connection, PersonRepository repository,
            SettingsRepository settings, Audit audit, Out<Person> person) throws BadRequestException {
        String by = CurrentUser.require(connection);
        String id = sheetId == null ? "" : sheetId.trim();
        if (!settings.role(id, by).equals(SettingsRepository.STEWARD)) {
            throw new BadRequestException("only a steward can flag a person");
        }
        long pid;
        try {
            pid = Long.parseLong(personId == null ? "" : personId.trim());
        } catch (NumberFormatException e) {
            throw new BadRequestException("no such person");
        }
        Person found = repository.find(id, pid);
        if (found == null) {
            throw new BadRequestException("no such person");
        }
        repository.flag(id, pid, input.reason(), input.until());
        audit.record("PERSON_FLAGGED sheet=" + id + " by=" + by + " name=" + found.name() + " reason=" + input.reason()
                + " until=" + input.until());
        person.set(repository.find(id, pid));
    }
}
