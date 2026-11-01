package net.officefloor.hq.app.voidreason;

import net.officefloor.hq.app.Audit;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

/** Adds a reason (trimmed) to the sheet's void reasons; refused for a blank one or one already on the list. */
public class AddVoidReason {
    public void add(@PathVariable("sheet") String sheet, @RequestParam("user") String user,
            @RequestBody VoidReasonRequest request, VoidReasons reasons, Audit audit) throws InvalidVoidReasonException {
        String reason = request == null || request.reason() == null ? "" : request.reason().trim();
        if (reason.isEmpty()) {
            throw new InvalidVoidReasonException("A void reason must not be blank");
        }
        if (reasons.has(sheet, reason)) {
            throw new InvalidVoidReasonException(reason + " is already a void reason");
        }
        reasons.add(sheet, reason);
        audit.record("VOID_REASON_ADDED sheet=" + sheet + " by=" + user + " reason=" + reason);
    }
}
