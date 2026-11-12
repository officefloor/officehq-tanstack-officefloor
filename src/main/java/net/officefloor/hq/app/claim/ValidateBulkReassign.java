package net.officefloor.hq.app.claim;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import net.officefloor.hq.app.sheet.InvalidSheetException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

public class ValidateBulkReassign {
    public void validate(@RequestBody BulkReassignRequest request, Out<BulkReassignInput> body) throws InvalidSheetException {
        if (request == null || request.claimIds() == null || request.claimIds().isEmpty()) {
            throw new InvalidSheetException("Select the claims to move");
        }
        String to = request.to() == null ? "" : request.to().trim();
        if (to.isEmpty()) {
            throw new InvalidSheetException("Choose the person to move the claims to");
        }
        long toId;
        try {
            toId = Long.parseLong(to);
        } catch (NumberFormatException e) {
            throw new InvalidSheetException("No such person: " + to);
        }
        Set<String> seen = new LinkedHashSet<>();
        List<Long> ids = new ArrayList<>();
        for (String raw : request.claimIds()) {
            String id = raw == null ? "" : raw.trim();
            if (!seen.add(id)) {
                continue;
            }
            Long parsed = null;
            try {
                parsed = Long.parseLong(id);
            } catch (NumberFormatException e) {
                // not an id: no such claim, refused
            }
            ids.add(parsed);
        }
        body.set(new BulkReassignInput(ids, toId, request.user()));
    }
}
