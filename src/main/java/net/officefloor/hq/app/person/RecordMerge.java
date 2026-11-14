package net.officefloor.hq.app.person;

import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestParam;

/** Records the merge about to be made, by the user on the sheet's today, so they can undo it that day. */
public class RecordMerge {
    public void record(@Val Merge merge, @RequestParam("user") String user, SheetRepository sheets,
            MergeRepository merges) {
        merges.record(merge, user, sheets.today(merge.from().sheetId()));
    }
}
