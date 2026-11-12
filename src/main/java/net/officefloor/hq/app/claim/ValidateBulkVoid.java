package net.officefloor.hq.app.claim;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import net.officefloor.hq.app.sheet.InvalidSheetException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

public class ValidateBulkVoid {
    public void validate(@RequestBody BulkVoidRequest request, Out<BulkVoidInput> body) throws InvalidSheetException {
        if (request == null || request.claimIds() == null || request.claimIds().isEmpty()) {
            throw new InvalidSheetException("Select the claims to void");
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
        String reason = request.reason() == null ? "" : request.reason().trim();
        body.set(new BulkVoidInput(ids, request.user(), reason));
    }
}
