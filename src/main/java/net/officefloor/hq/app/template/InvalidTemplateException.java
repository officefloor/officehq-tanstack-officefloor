package net.officefloor.hq.app.template;

/** The template's name is blank, or its amount is one the claim form would refuse. */
public class InvalidTemplateException extends Exception {
    public InvalidTemplateException(String message) {
        super(message);
    }
}
