package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.session.ChangedBy;
import net.officefloor.hq.app.sheet.SheetRepository;
import org.springframework.stereotype.Service;

/** Writes a claim's status log line once it has been added or moved: its status, who, the sheet's today and its due now. */
@Service
public class ClaimLog {

    private final ClaimLogRepository log;
    private final ClaimRepository claims;
    private final SheetRepository sheets;
    private final Rollup rollup;

    public ClaimLog(ClaimLogRepository log, ClaimRepository claims, SheetRepository sheets, Rollup rollup) {
        this.log = log;
        this.claims = claims;
        this.sheets = sheets;
        this.rollup = rollup;
    }

    public void record(Claim changed, ChangedBy by) {
        // Re-read as stored, so the due is the one the screen shows for it now.
        Claim claim = claims.find(changed.getSheetId(), changed.getId());
        log.add(claim, by.name(), sheets.today(claim.getSheetId()), rollup.dueOf(claim));
    }
}
