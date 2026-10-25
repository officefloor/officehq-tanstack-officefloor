package net.officefloor.hq.app.rate;

import java.util.List;
import java.util.Objects;
import net.officefloor.hq.app.AuditUser;
import net.officefloor.hq.app.role.Role;
import net.officefloor.hq.app.role.RoleRepository;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestHeader;

/** Withdraws the newest version of the grade's rate for the from-date on the sheet: the version before it, when there
 *  is one, applies again; with none the rate is gone. Its log line goes with it. Only a steward may; refuses a grade
 *  with no rate for the from-date. */
public class WithdrawRate {
    public void withdraw(@Val RateWithdrawal withdrawal, @Val Sheet sheet,
            @RequestHeader(name = "X-User", required = false) String user, RoleRepository roles,
            RateRepository repository, RateLogRepository log, RateVersionRepository versions) throws InvalidRateException {
        String role = roles.roleOf(sheet.getId(), AuditUser.decode(user));
        if (!Role.STEWARD.equals(role)) {
            throw new InvalidRateException("A " + role + " cannot withdraw a rate.");
        }
        Rate rate = repository.findWithFrom(sheet.getId(), withdrawal.grade(), withdrawal.from())
                .orElseThrow(() -> new InvalidRateException("The grade has no rate for that from-date."));
        List<RateLog> lines = log.findBySheetIdOrderByIdAsc(sheet.getId()).stream()
                .filter(l -> l.getGrade().equals(rate.getGrade()) && Objects.equals(l.getFromDate(), rate.getFromDate()))
                .toList();
        if (!lines.isEmpty()) {
            log.delete(lines.get(lines.size() - 1));
        }
        RateVersion older = versions.findNewest(sheet.getId(), rate.getGrade(), rate.getFromDate()).orElse(null);
        if (older == null) {
            repository.delete(rate);
        } else {
            rate.restore(older);
            repository.save(rate);
            versions.delete(older);
        }
        repository.flush();
    }
}
