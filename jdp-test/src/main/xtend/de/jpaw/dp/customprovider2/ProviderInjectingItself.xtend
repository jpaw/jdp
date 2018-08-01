package de.jpaw.dp.customprovider2

import de.jpaw.dp.Jdp
import de.jpaw.dp.Provider
import de.jpaw.dp.Singleton
import de.jpaw.dp.Startup
import de.jpaw.dp.StartupShutdown

class Dummy {
    new() {
        println("I am a dummy")
    }
}

@Startup(100)
@Singleton
class ProviderInjectingItself implements Provider<Dummy>, StartupShutdown {
    final Dummy myDummy = new Dummy();

    override get() {
        println("   using get()")
        return myDummy;
    }

    override onShutdown() {
        println("goodbye")
    }

    override onStartup() {
        Jdp.registerWithCustomProvider(Dummy, this)

        println("Getting a new instance")
        Jdp.getRequired(Dummy)
        Jdp.getProvider(Dummy).get
        println("Got it!")
    }

}

class ProviderInjectingItselfDemo {
    def static void main(String [] args) {
        Jdp.init("de.jpaw.dp.customprovider2")

        println("Getting a new provider")
        val provider = Jdp.getProvider(Dummy)
        val instance = provider.get
        Jdp.shutdown
    }
}
