package de.jpaw.dp.exceptions;

public abstract class JdpException extends RuntimeException {
    private static final long serialVersionUID = 4269451871043510544L;

    public JdpException(String msg, String qualifier) {
        super(qualifier == null ? msg : msg + " for qualifier <" + qualifier + ">");
    }

    public JdpException(Throwable cause, String msg) {
        super(msg, cause);
    }
}
