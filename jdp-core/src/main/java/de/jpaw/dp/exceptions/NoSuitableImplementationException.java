package de.jpaw.dp.exceptions;

/** Thrown when a required injection is desired, but none has been found. */
public class NoSuitableImplementationException extends JdpException {
    private static final long serialVersionUID = 8914500842486539013L;

    public NoSuitableImplementationException(Class<?> type) {
        super(type.getCanonicalName(), null);
    }
    public NoSuitableImplementationException(Class<?> type, String qualifier) {
        super(type.getCanonicalName(), qualifier);
    }

}
