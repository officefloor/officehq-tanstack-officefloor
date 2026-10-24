package net.officefloor.hq.app.template;

/** A template that cannot be saved: a blank name, or an amount the claim form would refuse. */
public class InvalidTemplateException extends Exception {
    public InvalidTemplateException(String message) {
        super(message);
    }
}
