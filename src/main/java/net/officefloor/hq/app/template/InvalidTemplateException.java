package net.officefloor.hq.app.template;

/** A template save with no signed-in user, a blank name, or an amount (or note) the claim form would refuse. */
public class InvalidTemplateException extends Exception {
    public InvalidTemplateException(String message) {
        super(message);
    }
}
