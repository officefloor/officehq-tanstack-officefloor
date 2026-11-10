package net.officefloor.hq.app.template;

/** A claim template with a blank name, or an amount (or note) the claim form would refuse. */
public class InvalidTemplateException extends Exception {
    public InvalidTemplateException(String message) {
        super(message);
    }
}
