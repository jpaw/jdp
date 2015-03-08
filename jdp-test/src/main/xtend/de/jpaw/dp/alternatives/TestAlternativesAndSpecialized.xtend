package de.jpaw.dp.alternatives

import de.jpaw.dp.Alternative
import de.jpaw.dp.Inject
import de.jpaw.dp.Jdp
import de.jpaw.dp.Singleton
import de.jpaw.dp.Specializes

// definition of an interface. Note that the interface itself is not annotated, same as in guice, CDI etc.
interface Car {
    def void drive();
}

@Alternative
@Singleton
class DumboBuggyShouldNotBeSelected implements Car {
    override drive() {
        println("no, I am an alternative!")
    }
}

@Singleton
class BaseCarShouldNotBeSelected implements Car {
    override drive() {
        println("no, I was overridden!")
    }
}

@Specializes
@Singleton
class MegaCarShouldBeSelected extends BaseCarShouldNotBeSelected {
    override drive() {
        println("I'm driving mega car!")
    }
}

@Alternative
@Singleton
class AnotherDumboBuggyShouldAlsoNotBeSelected implements Car {
    override drive() {
        println("no, I am an alternative!")
    }
}

@Alternative
@Singleton
class SomeCarWhichIsDrivenLater implements Car {
    override drive() {
        println("I'm doing the last round")
    }
}

class MainTestMain {
    def static void main(String [] args) {
        Jdp.init("de.jpaw.dp.alternatives");

        // access from Java code
        Jdp.getRequired(Car).drive();

        // access from xtend
        new SomeOtherClass().driveOtherCar

        // modify the preference
        Jdp.bindClassWithoutQualifier(SomeCarWhichIsDrivenLater, Car)

        // should result in a different one used from now on...
        new SomeOtherClass().driveOtherCar
    }
}


class SomeOtherClass {
    @Inject
    Car myCar

    def void driveOtherCar() {
        myCar.drive
    }
}
