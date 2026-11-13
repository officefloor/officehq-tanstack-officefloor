package net.officefloor.hq.app.watch;

/** A person and a user watching them, as the screen reads them. */
public record WatchResponse(long personId, String user) {

    public static WatchResponse from(Watch watch) {
        return new WatchResponse(watch.personId(), watch.user());
    }
}
