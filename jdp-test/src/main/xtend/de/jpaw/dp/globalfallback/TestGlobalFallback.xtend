package de.jpaw.dp.globalfallback

import de.jpaw.dp.Any
import de.jpaw.dp.Default
import de.jpaw.dp.Fallback
import de.jpaw.dp.Named
import de.jpaw.dp.Singleton

import static extension de.jpaw.dp.Jdp.*

// definition of an interface. Note that the interface itself is not annotated, same as in guice, CDI etc.
interface Car {
    def void drive(int number);
}

// a sample class, annotated with a DI scope. It's implementing an interface,
// to showcase that (no)injection works on the interface as well as on the class itself.
@Named("SUV")
@Singleton
class X3 implements Car {
    override drive(int number) {
        println("xdrive does " + number)
    }
}

@Named("SUV")
@Default
@Singleton
class GClass implements Car {
    override drive(int number) {
        println("xdrive does " + number)
    }
}

@Named("Sedan")
@Singleton
class CClass implements Car {
    override drive(int number) {
        println("Mr and Mrs Smith are driving " + number)
    }
}

@Named("SUV")
@Fallback
@Singleton
class Defender implements Car {
    override drive(int number) {
        println("LandRover does " + number)
    }
}

@Fallback
@Any
@Singleton
class DefaultCar implements Car {
    override drive(int number) {
        println("Any car could go " + number)
    }
}

class MainTestMain {
    def static void main(String [] args) {
        init("de.jpaw.dp.globalfallback");

        println(dump)

        val sedan = getOptional(Car, "Sedan")
        println('''The sedan is «sedan?.class?.simpleName ?: "null"»''')

        val anySuv = getAllClasses(Car, "SUV")
        val anyCar = getAllClassesAnyQualifier(Car)
        println('''The SUVs are «anySuv.map[simpleName].join(', ')»''')
        println('''All known cars are «anyCar.map[simpleName].join(', ')»''')

        val defaultSuv = getRequired(Car, "SUV")
        println('''The default SUV is «defaultSuv.class.simpleName ?: "null"»''')
        if (defaultSuv.class !== GClass)
            throw new Exception("Unexpected behaviour")

        val someUnknownCar = getRequired(Car, "Convertible")
        println('''I wanted a convertible but got a «someUnknownCar.class.simpleName ?: "null"»''')

        val oneCarPerQualifier = getOneInstancePerQualifier(Car)
        if (oneCarPerQualifier.size != 2)
            throw new Exception("Unexpected behaviour: expected 2 cars, one SUV, one Sedan")
        for (e : oneCarPerQualifier) {
            println('''One is a «e.class.simpleName»''')
        }

        val carMapPerQualifier = getInstanceMapPerQualifier(Car)
        if (carMapPerQualifier.size != 2)
            throw new Exception("Unexpected behaviour: expected 2 cars, one SUV, one Sedan")
        for (e : carMapPerQualifier.entrySet) {
            println('''Mapped to «e.key» is a «e.value.class.simpleName»''')
        }
    }
}
