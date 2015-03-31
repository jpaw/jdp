package de.jpaw.dp.customprovider

import de.jpaw.dp.CustomScope
import de.jpaw.dp.Jdp
import de.jpaw.dp.ScopeWithCustomProvider

interface Activity {
    def void doSomething();
}


// a scope which returns the same instance per day, but a new one the next day.
// for demonstration purposes, it does this for a second instead of a day
// This is for demonstration purposes, the shown class is not thread safe!
class DailyScope implements CustomScope<Activity> {
    private Activity currentActivity = null;
    private long whenSet = -1L;

    // defines the returned activity to be a specific one, from now on...
    override set(Activity instance) {
        currentActivity = instance;
        whenSet = System.currentTimeMillis / 1000
    }

    override close() {
        // hands over resources to GC
        currentActivity = null;
        whenSet = -1;
    }

    override get() {
        val now = System.currentTimeMillis / 1000
        if (currentActivity !== null && whenSet == now) {
            // there is a defined activity, and it was set within this second. Return it.
            return currentActivity
        }
        // create a new activity. Call some factory, or just create a new instance for this simple demo
        whenSet = now
        currentActivity = new Reading();
        return currentActivity
    }
}

@ScopeWithCustomProvider(DailyScope)
class Reading implements Activity {
    private int howOften = 0

    override doSomething() {
        howOften += 1
        println('''I'm reading page «howOften»''')
    }
}


class SimpleCustomScopeDemo {
    def static void main(String [] args) {
        Jdp.init("de.jpaw.dp.customprovider")

        for (var int i = 0; i < 10; i += 1) {
            Jdp.getRequired(Activity).doSomething
            Thread.sleep(300)       // pass some time...
        }
    }
}
