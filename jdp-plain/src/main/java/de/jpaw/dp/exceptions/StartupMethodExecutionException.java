package de.jpaw.dp.exceptions;

public class StartupMethodExecutionException extends JdpException {
    private static final long serialVersionUID = 8914500842486539018L;

    public StartupMethodExecutionException(Class<?> type, Throwable cause) {
        super(cause, type.getCanonicalName());
    }
}
