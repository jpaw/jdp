package de.jpaw.dp.exceptions;

public class ClassRegisteredTwiceException  extends JdpException {
    private static final long serialVersionUID = 891450084343539022L;

    public ClassRegisteredTwiceException(Class<?> type) {
        super(type.getCanonicalName(), null);
    }
}
