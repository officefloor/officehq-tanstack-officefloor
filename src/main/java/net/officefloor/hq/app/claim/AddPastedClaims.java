package net.officefloor.hq.app.claim;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.rollup.RollupBefore;
import net.officefloor.hq.app.setting.SettingRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

/** Adds the checked claims as DRAFTs in the order pasted, each as AddClaim adds one, writing CLAIM_ADDED for each and
 *  then the figures the paste moved (see Rollup). */
public class AddPastedClaims {
    public void add(@PathVariable("sheetId") String sheetId, @Val PastedClaims checked, @Val PastedLines pasted,
            @Val RollupBefore before, ClaimRepository claims, SettingRepository settings, ClaimLog log, Rollup rollup,
            Audit audit, Out<PasteClaimsResult> result) {
        List<String> lines = new ArrayList<>();
        for (PastedClaim c : checked.claims()) {
            LocalDate date = c.checked().date();
            String sheet = c.person().sheetId();
            Claim added = claims.add(sheet, c.person().id(), c.values().amount(), date, c.values().note(),
                    c.checked().corrects(), date.plusDays(settings.terms(sheet, c.person().terms(), c.person().gradeOn(date), date)));
            log.write(added, pasted.by());
            lines.add("CLAIM_ADDED sheet=" + added.sheetId() + " by=" + pasted.by() + " person=" + c.person().name()
                    + " amount=" + added.amount().toPlainString() + " date=" + added.date());
        }
        lines.forEach(audit::record);
        rollup.record(sheetId.trim(), pasted.by(), before);
        result.set(new PasteClaimsResult(checked.claims().size()));
    }
}
