package de.jpaw.dp.exceptions;

public class StartupBeanInstantiationException extends JdpException {
    private static final long serialVersionUID = 8914500842486539018L;

    public StartupBeanInstantiationException(Class<?> type, Throwable cause) {
        super(cause, type.getCanonicalName());
    }
}
