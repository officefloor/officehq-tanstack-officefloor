package net.officefloor.hq.app.watch;

import java.util.List;
import net.officefloor.hq.app.claim.Claim;
import net.officefloor.hq.app.claim.ClaimStatus;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.sheet.Sheet;

/**
 * The notify lines for a claim settled (approved, re-settled): one per watcher of its person, in
 * the order they started watching. A claim not SETTLED (sent to PENDING) notifies no one.
 */
public final class WatchNotices {
    private WatchNotices() {}

    public static List<String> lines(Sheet sheet, Claim claim, String user, PersonRepository people,
            WatchRepository watches) {
        if (claim.getStatus() != ClaimStatus.SETTLED) return List.of();
        String name = people.find(sheet.id(), claim.getPersonId()).getName();
        return watches.watchers(sheet.id(), claim.getPersonId()).stream()
                .map((w) -> "NOTIFY sheet=" + sheet.id() + " by=" + user + " user=" + w + " person=" + name
                        + " number=" + claim.getNumber())
                .toList();
    }
}
