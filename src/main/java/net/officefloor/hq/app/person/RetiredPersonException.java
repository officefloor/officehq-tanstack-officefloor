package net.officefloor.hq.app.person;

/** A claim dated after its person's retirement date: it cannot be added, nor edited to that date. */
public class RetiredPersonException extends Exception {
    public RetiredPersonException(String name) {
        super(name + " is retired: a claim cannot be dated after their retirement date");
    }
}
