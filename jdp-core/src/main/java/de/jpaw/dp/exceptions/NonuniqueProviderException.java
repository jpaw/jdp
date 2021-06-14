package de.jpaw.dp.exceptions;

public class NonuniqueProviderException extends JdpException {
    private static final long serialVersionUID = 8914500842486539022L;

    public NonuniqueProviderException(Class<?> type) {
        super(type.getCanonicalName(), null);
    }
    public NonuniqueProviderException(Class<?> type, String qualifier) {
        super(type.getCanonicalName(), qualifier);
    }
}
