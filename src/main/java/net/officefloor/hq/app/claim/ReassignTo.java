package net.officefloor.hq.app.claim;

/** The id of the person a claim is to move to; once moved, the names it moved from and to, for the audit. */
public class ReassignTo {

    private final long personId;
    private String fromName;
    private String toName;

    public ReassignTo(long personId) {
        this.personId = personId;
    }

    public long personId() {
        return personId;
    }

    public String fromName() {
        return fromName;
    }

    public String toName() {
        return toName;
    }

    void moved(String fromName, String toName) {
        this.fromName = fromName;
        this.toName = toName;
    }
}
