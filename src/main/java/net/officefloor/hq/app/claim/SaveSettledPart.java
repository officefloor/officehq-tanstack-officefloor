package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.person.WatchRepository;
import net.officefloor.hq.app.rate.RateRepository;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.setting.SettingRepository;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestParam;

/** After the split is saved and recorded: settles the split-off claim as the settle button does — saved
 *  in no batch, its status logged, its line recorded, its person's watchers notified, then the rollup. */
public class SaveSettledPart {
    public void save(@RequestParam(name = "by", required = false) String by, @Val Split split,
            @Val PartSettle part, ClaimRepository claims, ClaimLogRepository log, PersonRepository people,
            WatchRepository watches, RateRepository rates, SheetRepository sheets, SettingRepository settings,
            PeriodRepository periods, Audit audit) {
        Claim created = split.created();
        String sheetId = created.getSheetId();
        Rollup before = Rollup.of(sheetId, people, claims, rates, sheets, settings);
        created.setStatus(part.status());
        created.setSettledDue(part.settledDue());
        created.setSettleBatch(null);
        ClaimNumbers.settled(created, claims, periods, settings, sheets);
        claims.update(created);
        log.record(created, by);
        new RecordClaimStatus().record(by, created, people, audit);
        new RecordClaimNotify().record(by, created, people, watches, audit);
        Rollup after = Rollup.of(sheetId, people, claims, rates, sheets, settings);
        for (String line : after.changesSince(before, sheetId, by == null ? "" : by)) {
            audit.record(line);
        }
    }
}
