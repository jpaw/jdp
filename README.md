jdp
===

Java dependency provider - a "no DI" framework


## Purpose

JDP intends to be used as a lean alternative to classic dependency injection frameworks such as guice or CDI.
It does not attempt to implement a full JSR 330 or 299 standard.

JDP provides the following annotations:

* Inject

* Singleton

* Dependent

* PerThread (planned)

* Named

JDP offers the following features:

* No need for a no-args constructor. In fact you can create classes with a regular new().

* No use of proxies. This allows you to have final methods and classes.

* Modifying object resultion at runtime. Fields injected before will continue to have their original value, though.

JDP does not work with cyclic dependencies. I consider that bad design anyway.


# How does it work

JDP is intended to be used on Xtend source code. @Inject is an active annotation which provides the annotated field
with an initializer, which is just a library call to obtain the desired value. That simple, but sufficient for some
of my applications.


