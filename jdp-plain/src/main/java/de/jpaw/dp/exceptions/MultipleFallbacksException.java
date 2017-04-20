package de.jpaw.dp.exceptions;

public class MultipleFallbacksException extends JdpException {
    private static final long serialVersionUID = 8914500842486539022L;

    public MultipleFallbacksException(Class<?> type) {
        super(type.getCanonicalName(), null);
    }
    public MultipleFallbacksException(Class<?> type, String qualifier) {
        super(type.getCanonicalName(), qualifier);
    }
}
