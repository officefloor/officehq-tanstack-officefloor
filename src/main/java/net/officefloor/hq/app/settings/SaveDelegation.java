package net.officefloor.hq.app.settings;

import java.time.LocalDate;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.CurrentUser;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import net.officefloor.server.http.ServerHttpConnection;
import org.springframework.web.bind.annotation.PathVariable;

/** A steward (by their own role, not one delegated to them) delegates the steward role to a user for a range of
 *  dates; a to-date before the from-date (blank: the sheet's today) is refused. */
public class SaveDelegation {
    public void save(@PathVariable("sheetId") String sheetId, @Val DelegationInput input, ServerHttpConnection connection,
            SettingsRepository repository, Audit audit, Out<Settings> settings) throws BadRequestException {
        String by = CurrentUser.require(connection);
        String id = sheetId == null ? "" : sheetId.trim();
        if (!repository.sheetExists(id)) {
            throw new BadRequestException("no such sheet");
        }
        if (!repository.ownRole(id, by).equals(SettingsRepository.STEWARD)) {
            throw new BadRequestException("only a steward can delegate");
        }
        if (input.user().equals(by)) {
            throw new BadRequestException("a steward cannot delegate to themselves");
        }
        LocalDate from = input.from() == null ? repository.today(id) : input.from();
        if (input.to().isBefore(from)) {
            throw new BadRequestException("to must not be before from");
        }
        repository.delegate(id, input.user(), by, from, input.to());
        audit.record("DELEGATED sheet=" + id + " by=" + by + " user=" + input.user() + " from=" + from + " to=" + input.to());
        settings.set(repository.load(id));
    }
}
