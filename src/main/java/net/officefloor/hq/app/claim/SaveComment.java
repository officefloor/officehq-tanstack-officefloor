package net.officefloor.hq.app.claim;

import java.util.List;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/** Keep the comment on the loaded claim, with its writer and the sheet's today; a claim of any status may be
 *  commented. */
public class SaveComment {
    public void save(@Val CommentInput input, @Val Claim loaded, SheetRepository sheets, ClaimCommentRepository comments,
            Out<ClaimResponse> saved) {
        var today = sheets.today(loaded.sheetId());
        comments.add(loaded, input.user(), today, input.text());
        saved.set(ClaimResponse.from(loaded, List.of(), today,
                comments.lines(loaded.sheetId()).getOrDefault(loaded.id(), List.of())));
    }
}
