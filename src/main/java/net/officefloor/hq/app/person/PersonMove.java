package net.officefloor.hq.app.person;

/** A validated move: up (or down), by the user; position is the person's new place in the list (counting from 1),
 *  set once moved. */
public class PersonMove {
    private final boolean up;
    private final String by;
    private int position;

    public PersonMove(boolean up, String by) {
        this.up = up;
        this.by = by;
    }

    public boolean up() {
        return up;
    }

    public String by() {
        return by;
    }

    public int position() {
        return position;
    }

    public void moved(int position) {
        this.position = position;
    }
}
