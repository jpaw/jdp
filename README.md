jdp
===

Java dependency provider - a "no DI" framework


## Purpose

JDP intends to be used as a lean alternative to classic dependency injection frameworks such as guice or CDI.
It does not attempt to implement a full JSR 330 or 299 standard.

JDP provides the following annotations:


* Inject (only for xtend) - transparently invokes the provider. @Inject @Any and @Inject @Optional are supported as well.

* Singleton (scope) - the provider returns the same instance every time

* Dependent (scope) - the provider returns a new instance every time

* PerThread (planned)

* Named (qualifier) - assigns the class to a specific category of implementations

* Specialized (qualifier) - overrides the implementation of any inherited subclass

* Alternative (qualifier) - prevents the annotated class to be used unless it is the only one for the category

JDP offers the following features:

* Autodetection of classes, as with CDI. No need to code modules as in guice.

* No need for a no-args constructor. In fact you can create classes with a regular new().

* No use of proxies as in CDI. This allows you to have final methods and classes.

* Modifying object resolution at runtime. Fields injected before will continue to have their original value, though.

JDP does not work with cyclic dependencies. I consider that bad design anyway.


## How does it work

JDP is intended to be used on Xtend source code. @Inject is an active annotation which provides the annotated field
with an initializer, which is just a library call to obtain the desired value. That simple, but sufficient for some
of my applications. The xtend portion will be split into a separate project and therefore optional in the next revision.

It offers enough features to replace CDI in some multi-million code line projects.
The class repository is kept in a static class however and therefore Jdp should only be used in projects
where you provide the main() entry point as well.


## Example

Let's look at some example code. The code is in xtend, which is very similar to Java, and everything except the @Inject
will look exactly the same in Java.

```java
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
```

Running the code will produce
```text
I'm driving mega car!
I'm driving mega car!
I'm doing the last round
```

