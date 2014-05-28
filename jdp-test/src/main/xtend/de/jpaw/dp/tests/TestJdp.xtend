package de.jpaw.dp.tests

import de.jpaw.dp.Inject
import de.jpaw.dp.Jdp
import de.jpaw.dp.Singleton

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
// a class using some (no)injections
class Test2 {
	@Inject Author myAuthor
	@Inject Shakespeare alsoAuthor
	
	def public runIt(int number) {
		myAuthor.doWrite(number)
		alsoAuthor.doWrite(number)
	}
	 
}

class Main {
	def static void main(String [] args) {
		Jdp.init("de.jpaw.dp.tests");
		(new Test2).runIt(8)		// showcase that regular new() is working
	}
}


