package net.officefloor.hq.app.rollover;

import java.time.LocalDate;
import java.util.List;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.claim.Claim;
import net.officefloor.hq.app.claim.ClaimRepository;
import net.officefloor.hq.app.period.InvalidPeriodException;
import net.officefloor.hq.app.period.Period;
import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.hq.app.setting.Role;
import net.officefloor.hq.app.setting.SettingRepository;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Takes the sheet's last rollover back and records ROLLOVER_UNDONE. Refused (400) when there is none left to undo, it
 * was made on another sheet-today or by another user, the user no longer acts as a steward, or a claim (of any status)
 * belongs to a period it generated.
 */
public class UndoRollover {
    public void undo(@Val Sheet sheet, @RequestParam("user") String user, RolloverRepository repository,
            PeriodRepository periods, ClaimRepository claims, SettingRepository settings, Audit audit,
            Out<RolloverRecord> undone) throws InvalidPeriodException {
        RolloverRecord last = repository.last(sheet.id());
        if (last == null || last.undone()) {
            throw new InvalidPeriodException("There is no rollover to undo.");
        }
        if (settings.find(sheet.id()).roleOf(user) != Role.STEWARD || !last.madeBy().equals(user)) {
            throw new InvalidPeriodException("Only the steward who rolled the year over may undo it.");
        }
        if (!last.madeOn().equals(sheet.today())) {
            throw new InvalidPeriodException("A rollover can only be undone on the day it was made.");
        }
        List<Period> generated = periods.findAll(sheet.id()).stream()
                .filter((p) -> last.periodNames().contains(p.name())).toList();
        for (Claim claim : claims.findAll(sheet.id())) {
            LocalDate on = claim.dueBy() != null ? claim.dueBy() : claim.date();
            if (generated.stream().anyMatch((p) -> p.contains(on))) {
                throw new InvalidPeriodException("A claim belongs to a period the rollover made.");
            }
        }
        repository.undo(last);
        audit.record("ROLLOVER_UNDONE sheet=" + sheet.id() + " by=" + user + " fy=" + last.fy());
        undone.set(last);
    }
}
