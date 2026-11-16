package net.officefloor.hq.app.claim;

import java.util.List;
import java.util.Objects;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Reads the body: the selected claim ids (each once; a missing list is none) and who opens them. */
public class ValidateBulkOpen {
    public void validate(@RequestBody BulkOpenRequest request, Out<BulkOpenRequest> body) {
        List<Long> ids = request == null || request.claimIds() == null ? List.of()
                : request.claimIds().stream().filter(Objects::nonNull).distinct().toList();
        String by = request == null || request.by() == null ? "" : request.by().trim();
        body.set(new BulkOpenRequest(ids, by));
    }
}
