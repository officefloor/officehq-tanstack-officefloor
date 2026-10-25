package net.officefloor.hq.app;

/** A request the app refuses; answered 400 with the message (see HandleBadRequest). */
public class BadRequestException extends Exception {
    public BadRequestException(String message) {
        super(message);
    }
}
