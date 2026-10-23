package net.officefloor.hq.app.claim;

import java.util.List;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/** Keep the line on the loaded claim and make its amount the lines' total, recording the user as its last editor. */
public class SaveLine {
    public void save(@Val LineInput input, @Val Claim loaded, SheetRepository sheets, ClaimRepository repository,
            ClaimLineRepository lines, Out<ClaimResponse> saved) {
        var today = sheets.today(loaded.sheetId());
        lines.add(loaded, input.label(), input.qty(), input.unit(), today);
        var total = lines.total(loaded);
        repository.lined(loaded, total, input.user());
        Claim claim = repository.find(loaded.sheetId(), loaded.id());
        saved.set(ClaimResponse.from(claim, List.of(), today, List.of(), null,
                lines.lines(loaded.sheetId()).getOrDefault(loaded.id(), List.of())));
    }
}
