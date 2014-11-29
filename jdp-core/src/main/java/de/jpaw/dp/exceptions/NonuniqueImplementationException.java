package de.jpaw.dp.exceptions;

public class NonuniqueImplementationException extends JdpException {
    private static final long serialVersionUID = 8914500842486539012L;

    public NonuniqueImplementationException(Class<?> type) {
        super(type.getCanonicalName(), null);
    }
    public NonuniqueImplementationException(Class<?> type, String qualifier) {
        super(type.getCanonicalName(), qualifier);
    }
}
