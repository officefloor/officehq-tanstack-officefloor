package net.officefloor.hq.app.sheet;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

/**
 * The sheet's descendants: the sheets naming it as their parent, the sheets naming those, and so
 * on. Each is listed once, and never the sheet itself.
 */
public class RespondWithDescendants {
    public void respond(@Val Sheet sheet, SheetRepository repository, ObjectResponse<DescendantsResponse> response) {
        List<String> descendants = new ArrayList<>();
        Set<String> seen = new HashSet<>(Set.of(sheet.id()));
        Deque<String> pending = new ArrayDeque<>(List.of(sheet.id()));
        while (!pending.isEmpty()) {
            for (String child : repository.children(pending.removeFirst())) {
                if (seen.add(child)) {
                    descendants.add(child);
                    pending.addLast(child);
                }
            }
        }
        response.send(new DescendantsResponse(descendants));
    }
}
