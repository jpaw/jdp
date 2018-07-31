package de.jpaw.dp.namedtests

import de.jpaw.dp.Jdp
import de.jpaw.dp.Named
import de.jpaw.dp.Singleton

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

@Named("Sedan")
@Singleton
class CClass implements Car {
    override drive(int number) {
        println("Mr and Mrs Smith are driving " + number)
    }
}

@Named("SUV")
@Singleton
class Defender implements Car {
    override drive(int number) {
        println("LandRover does " + number)
    }
}

@Named("SUV")
@Named("Helicopter")
@Named("Boat")
@Singleton
class FlyWaToot implements Car {
    override drive(int number) {
        println("Robbi got a really good one! " + number)
    }
}

class MainTestMain {
    def static void main(String [] args) {
        Jdp.init("de.jpaw.dp.namedtests");

        println(Jdp.dump)

        val sedan = Jdp.getOptional(Car, "Sedan")
        println('''The sedan is «sedan?.class?.simpleName ?: "null"»''')

        val anySuv = Jdp.getAllClasses(Car, "SUV")
        val anyCar = Jdp.getAllClassesAnyQualifier(Car)
        println('''The SUVs are «anySuv.map[simpleName].join(', ')»''')
        println('''All known cars are «anyCar.map[simpleName].join(', ')»''')
    }
}
