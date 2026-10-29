package net.officefloor.hq.app.person;

/** A move that may not happen: the top person moved up, the bottom person moved down, or no such direction. */
public class PersonMoveRefusedException extends Exception {

    public PersonMoveRefusedException(String message) {
        super(message);
    }
}
