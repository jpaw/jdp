package de.jpaw.dp;

/** xtend extension methods - just syntactic sugar for better readability. */
public class JdpExtensions {

    // bind type to interface or child to superclass

    public static <I> void isNow(Class<I> interfaze, Class<? extends I> implementation) {
        Jdp.bindClassWithoutQualifier(implementation, interfaze);
    }

    public static <I> void isNow(Class<I> interfaze, Class<? extends I> implementation, String qualifier) {
        Jdp.bindClassToQualifier(implementation, interfaze, qualifier);
    }



    // bind instance to interface or child to superclass

    public static <I> void isNow(Class<I> interfaze, I implementationInstance) {
        Jdp.bindInstanceTo(implementationInstance, interfaze);
    }

    public static <I> void isNow(Class<I> interfaze, I implementationInstance, String qualifier) {
        Jdp.bindInstanceTo(implementationInstance, interfaze, qualifier);
    }



    // resolvers for instances - opt and required, with / without qualifier

    public static <I> void getInstance(Class<I> interfaze) {
        Jdp.getRequired(interfaze);
    }

    public static <I> void getInstance(Class<I> interfaze, String qualifier) {
        Jdp.getRequired(interfaze, qualifier);
    }

    public static <I> void getInstanceOrNull(Class<I> interfaze) {
        Jdp.getOptional(interfaze);
    }

    public static <I> void getInstanceOrNull(Class<I> interfaze, String qualifier) {
        Jdp.getOptional(interfaze, qualifier);
    }



    // resolvers for providers - opt and required, with / without qualifier - essentially only duplicated from Jdp, to avoid an additional import

    public static <I> Provider<I> getProvider(Class<I> interfaze) {
        return Jdp.getProvider(interfaze, null);
    }

    public static <I> Provider<I> getProvider(Class<I> interfaze, String qualifier) {
        return Jdp.getProvider(interfaze, qualifier);
    }

    public static <I> Provider<I> getProviderOrNull(Class<I> interfaze) {
        return Jdp.getOptionalProvider(interfaze, null);
    }

    public static <I> Provider<I> getProviderOrNull(Class<I> interfaze, String qualifier) {
        return Jdp.getOptionalProvider(interfaze, qualifier);
    }
}
