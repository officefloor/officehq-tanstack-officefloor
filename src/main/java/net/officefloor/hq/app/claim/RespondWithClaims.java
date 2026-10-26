package net.officefloor.hq.app.claim;

import java.util.List;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;

public class RespondWithClaims {
    public void respond(@PathVariable("sheetId") String sheetId, Claims claims, ClaimLocks locks,
            ObjectResponse<List<ClaimResponse>> response) {
        java.util.Map<Long, List<String>> logs = claims.logs(sheetId.trim());
        java.util.Map<Long, List<String>> comments = claims.comments(sheetId.trim());
        java.util.Map<Long, String> locked = locks.list(sheetId.trim());
        response.send(claims.list(sheetId.trim()).stream()
                .map(c -> ClaimResponse.from(c, logs.getOrDefault(c.id(), List.of())).withLockedBy(locked.get(c.id()))
                        .withComments(comments.getOrDefault(c.id(), List.of())))
                .toList());
    }
}
