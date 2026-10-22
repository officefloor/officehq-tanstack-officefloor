package net.officefloor.hq.app.watch;

/** Body of watching a person: who is (un)watching, and whether they now watch them. */
public record WatchRequest(String user, Boolean watched) {
}
