package de.jpaw.dp.exceptions;

public class MultipleDefaultsException extends JdpException {
    private static final long serialVersionUID = 8914500842486539022L;

    public MultipleDefaultsException(Class<?> type) {
        super(type.getCanonicalName(), null);
    }
    public MultipleDefaultsException(Class<?> type, String qualifier) {
        super(type.getCanonicalName(), qualifier);
    }
}
