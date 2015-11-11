package de.jpaw.dp.generics.tests

import de.jpaw.dp.Inject
import java.util.List

// a class injecting some interface with generics parameter
class Test2 {
    @Inject List<String> myStringList
}


// a class injecting some interface with generics parameter
class Test3 {
    @Inject java.util.List<String> myStringList
}
