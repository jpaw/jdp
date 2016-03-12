package de.jpaw.dp.testStartupOk

import de.jpaw.dp.Jdp
import de.jpaw.dp.Startup

@Startup(17)
class Test17 {
    def static public void onStartup() {
        println("17 has been called")
    }
}
@Startup(170)
class Test170 {
    def static public void onStartup() {
        println("170 has been called")
    }
}
@Startup(3)
class Test3 {
    def static public void onStartup() {
        println("3 has been called")
    }
}

class Main {
    def static void main(String [] args) {
        Jdp.init("de.jpaw.dp.testStartupOk");
    }
}
