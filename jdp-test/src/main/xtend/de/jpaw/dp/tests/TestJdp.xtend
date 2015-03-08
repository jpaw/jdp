package de.jpaw.dp.tests

import de.jpaw.dp.Inject
import de.jpaw.dp.Jdp
import de.jpaw.dp.Singleton
import de.jpaw.dp.Alternative
import de.jpaw.dp.Any
import java.util.List

// definition of an interface. Note that the interface itself is not annotated, same as in guice, CDI etc.
interface Author {
    def void doWrite(int number);
}

// a sample class, annotated with a DI scope. It's implementing an interface,
// to showcase that (no)injection works on the interface as well as on the class itself.
@Singleton
class Shakespeare implements Author {
    new () {
        System.out.println("Author Shakespeare has been created")
    }
    override doWrite(int number) {
        println("Author Shakespeare is writing " + number)
    }

}

@Singleton
@Alternative
class Goethe implements Author {
    new () {
        println("Author Goethe has been created")
    }
    override doWrite(int number) {
        println("Author Goethe is writing " + number)
    }

}

// a class using some (no)injections
class Test2 {
    @Inject Author myAuthor
    @Inject Shakespeare alsoAuthor
    @Inject @Any List<Author> allAuthors

    def public runIt(int number) {
        // inject by interface
        myAuthor.doWrite(number)

        // inject by class type
        alsoAuthor.doWrite(number)

        // inject all (any)
        if (allAuthors !== null)
            for (a: allAuthors)
                a.doWrite(3)
    }

    def public runOthers() {
        // get instance for String name
        Jdp.getInstanceForClassname(Author, "de.jpaw.dp.tests.Goethe").doWrite(28);
        Jdp.getInstanceForClassname(Author, "de.jpaw.dp.tests.Shakespeare").doWrite(28);
    }

}

class MainTestMain {
    def static void main(String [] args) {
        Jdp.init("de.jpaw.dp.tests");
        new Test2 => [
            runIt(8)        // showcase that regular new() is working
            runOthers
        ]

        println(Jdp.dump)
    }
}
