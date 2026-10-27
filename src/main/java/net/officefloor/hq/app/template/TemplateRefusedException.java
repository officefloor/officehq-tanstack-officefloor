package net.officefloor.hq.app.template;

/** A claim template that may not be saved: a blank name, or an amount the claim form would refuse. */
public class TemplateRefusedException extends Exception {

    public TemplateRefusedException(String message) {
        super(message);
    }
}
