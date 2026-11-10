package net.officefloor.hq.app.person;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashSet;
import java.util.Set;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.CurrentUser;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.settings.SettingsRepository;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.server.http.ServerHttpConnection;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

/** Hold the selected people together from a from-date (blank: the sheet's today) to an optional to-date
 *  (blank: open-ended), in list order (by their position), each exactly as the row's hold would:
 *  PERSON_HELD and its rollup lines. An id that is no person on the sheet is skipped. A viewer's selection
 *  is all refused: no one is held. An archived sheet refuses it. */
public class BulkHoldPeople {
    public void hold(@PathVariable("sheetId") String sheetId, @RequestBody BulkHoldRequest request,
            ServerHttpConnection connection, PersonRepository repository, SheetRepository sheets, Audit audit,
            Rollup rollup, SettingsRepository settings, Out<BulkHoldResult> result) throws BadRequestException {
        String by = CurrentUser.require(connection);
        String id = sheetId == null ? "" : sheetId.trim();
        Set<Long> selected = new LinkedHashSet<>();
        if (request != null && request.personIds() != null) {
            for (Long pid : request.personIds()) {
                if (pid != null) selected.add(pid);
            }
        }
        if (selected.isEmpty()) {
            throw new BadRequestException("select at least one person");
        }
        LocalDate given = date(request.from(), "from");
        LocalDate to = date(request.to(), "to");
        LocalDate from = given != null ? given : sheets.today(id);
        if (to != null && to.isBefore(from)) {
            throw new BadRequestException("to must not be before from");
        }
        if (settings.archived(id)) {
            throw new BadRequestException("the sheet is archived");
        }
        int held = 0;
        if (settings.role(id, by).equals(SettingsRepository.VIEWER)) {
            result.set(new BulkHoldResult(held));
            return;
        }
        for (Person current : repository.list(id)) {
            if (!selected.contains(current.id())) continue;
            Rollup.Snapshot before = rollup.take(id);
            repository.hold(id, current.id(), from, to);
            audit.record("PERSON_HELD sheet=" + id + " by=" + by + " name=" + current.name() + " from=" + from);
            rollup.record(id, by, before);
            held++;
        }
        result.set(new BulkHoldResult(held));
    }

    private static LocalDate date(String value, String field) throws BadRequestException {
        String raw = value == null ? "" : value.trim();
        if (raw.isEmpty()) return null;
        try {
            return LocalDate.parse(raw);
        } catch (DateTimeParseException e) {
            throw new BadRequestException(field + " must be yyyy-mm-dd");
        }
    }
}
