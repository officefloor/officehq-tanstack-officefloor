package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.rate.RateRepository;
import net.officefloor.hq.app.setting.SettingRepository;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestParam;

/** Appends the claim's new status to its log: by the user, on the sheet's today, with its due at this moment. */
public class LogClaimStatus {
    public void log(@Val Claim claim, @RequestParam("user") String user, SheetRepository sheets, PersonRepository people,
            RateRepository rates, SettingRepository settings, ClaimRepository claims, ClaimLogRepository logs) {
        BigDecimal due = ClaimDueNow.of(claim, sheets, people, rates, settings, claims);
        logs.append(claim.sheetId(), new ClaimLogLine(claim.id(), claim.status(), user, sheets.today(claim.sheetId()),
                due == null ? "n/a" : due.setScale(2).toPlainString()));
    }
}
