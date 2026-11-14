package net.officefloor.hq.app.snapshot;

/** A person's snapshot line, as the screen reads it: the day being left (ISO) and their due-total on it. */
public record SnapshotResponse(long personId, String day, String dueTotal) {

    public static SnapshotResponse from(Snapshot snapshot) {
        return new SnapshotResponse(snapshot.personId(), snapshot.day().toString(), snapshot.dueTotal());
    }
}
