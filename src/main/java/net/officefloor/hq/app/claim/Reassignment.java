package net.officefloor.hq.app.claim;

/** A validated move of a claim: the person it moves to and, once applied, the names it moved from and to. */
public class Reassignment {
    private final long toPersonId;
    private String fromName;
    private String toName;

    public Reassignment(long toPersonId) {
        this.toPersonId = toPersonId;
    }

    public long getToPersonId() {
        return toPersonId;
    }

    public String getFromName() {
        return fromName;
    }

    public String getToName() {
        return toName;
    }

    void moved(String fromName, String toName) {
        this.fromName = fromName;
        this.toName = toName;
    }
}
