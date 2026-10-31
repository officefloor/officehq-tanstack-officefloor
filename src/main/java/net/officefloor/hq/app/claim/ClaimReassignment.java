package net.officefloor.hq.app.claim;

/** A validated reassignment body; `from` and `to` are the names of the claim's old and new person, set once it is applied. */
public class ClaimReassignment {
    private final long toPersonId;
    private final String user;
    private String from;
    private String to;

    public ClaimReassignment(long toPersonId, String user) {
        this.toPersonId = toPersonId;
        this.user = user;
    }

    public long toPersonId() { return toPersonId; }
    public String user() { return user; }
    public String from() { return from; }
    public String to() { return to; }

    public void moved(String from, String to) {
        this.from = from;
        this.to = to;
    }
}
