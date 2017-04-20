package de.jpaw.dp.exceptions;

public class CannotCreateProviderException extends JdpException {
    private static final long serialVersionUID = 89145008166539012L;

    public CannotCreateProviderException(Class<?> type, Class<?> providerType, Throwable cause) {
        super(cause, type.getCanonicalName() + " / " + providerType.getCanonicalName());
    }
}
