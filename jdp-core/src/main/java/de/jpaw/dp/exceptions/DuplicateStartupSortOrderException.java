package de.jpaw.dp.exceptions;

public class DuplicateStartupSortOrderException extends JdpException {
    private static final long serialVersionUID = 8914500842486539016L;

    public DuplicateStartupSortOrderException(Class<?> first, Class<?> second, Integer order) {
        super(order.toString() + " defined for " + first.getCanonicalName() + " as well as " + second.getCanonicalName(),  null);
    }
}
