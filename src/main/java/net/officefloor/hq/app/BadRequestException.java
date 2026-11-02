package net.officefloor.hq.app;

/** A refused request: answered 400 with its message (escalation/…BadRequestException.yml). */
public class BadRequestException extends Exception {
    public BadRequestException(String message) {
        super(message);
    }
}
