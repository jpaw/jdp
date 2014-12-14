package de.jpaw.dp.alternatives

import de.jpaw.dp.Alternative
import de.jpaw.dp.Jdp
import de.jpaw.dp.Singleton
import de.jpaw.dp.Specializes

// definition of an interface. Note that the interface itself is not annotated, same as in guice, CDI etc.
interface Car {
    def void drive();
}

// a sample class, annotated with a DI scope. It's implementing an interface,
// to showcase that (no)injection works on the interface as well as on the class itself. 
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
        println("no, I was overrideen!")
    }
} 

@Specializes
@Singleton
class MegaCarShouldBeSelected extends BaseCarShouldNotBeSelected {
    override drive() {
        println("yes!")
    }
} 

@Alternative
@Singleton
class AnotherDumboBuggyShouldAlsoNotBeSelected implements Car {
    override drive() {
        println("no, I am an alternative!")
    }
} 

class MainTestMain {
    def static void main(String [] args) {
        Jdp.init("de.jpaw.dp.alternatives");
        
//        println(Jdp.dump)
        
        Jdp.getRequired(Car).drive();
    }
}
