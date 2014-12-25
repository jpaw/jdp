package de.jpaw.dp.tests

import static extension de.jpaw.dp.Jdp.*

class LambdaMain {
    def static void main(String [] args) {
        init("de.jpaw.dp.tests");
        Author.forAll [ doWrite(666) ]
    }
}
