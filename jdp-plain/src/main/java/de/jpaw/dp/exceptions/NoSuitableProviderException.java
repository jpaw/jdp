package de.jpaw.dp.exceptions;

public class NoSuitableProviderException extends JdpException {
    private static final long serialVersionUID = 8914500345248659013L;

    public NoSuitableProviderException(Class<?> type) {
        super(type.getCanonicalName(), null);
    }
    public NoSuitableProviderException(Class<?> type, String qualifier) {
        super(type.getCanonicalName(), qualifier);
    }

}
