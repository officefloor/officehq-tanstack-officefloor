package net.officefloor.hq.app.template;

/** A claim template that cannot be saved (a blank name, an amount the claim form refuses); nothing is written. */
public class InvalidTemplateException extends Exception {
    public InvalidTemplateException(String message) {
        super(message);
    }
}
