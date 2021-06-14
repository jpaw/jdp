package de.jpaw.dp.exceptions;

public class MissingOnStartupMethodException extends JdpException {
    private static final long serialVersionUID = 8914500842486539018L;

    public MissingOnStartupMethodException(Class<?> type, Throwable cause) {
        super(cause, type.getCanonicalName());
    }
}
