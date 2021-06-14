Basic ideas:
============

bonaparte-di is a "no dependency injection" library, but aiming to mimic the DI syntax, using @Inject etc.

The concept is to use active annotations which translate the annotated code, to invoke initializers.

The main ideas are:
- no proxies.  A proxy is just a hidden Provider. Proxies are bad, because they prevent classes from using the final keyword (for
  classes as well as methods). This removes useful functionality. Whenever a field of a high-frequency scope is injected in a low-frequency
  scope class, it really means that a provider is injected. All providers are ApplicationScoped / Singletons
- Injection of any data type, not just interfaces / classes.   bonaparte DI allows to inject wrapper types such as Longs or Strings
  in a typesafe manner.
- Objects can be instantiated using new()!  Because code is generated as initializer, class instances can be created using a regular new(),
  and still any injections will be done.
  This also makes explicit class injections redundant, because the new() (with any parameter constructor) can be used, as ususal.
- default bindings. Same as in CDI, if there is just one implementing class, then injections to interfaces will use that class.
- runtime bindings. Same as in Guice, code can alter bindings at runtime and all subsequent injections will use the new binding

Scopes:
=======
built-in scopes:
@Singleton / ApplicationScoped
@PerThread
@Dependent (per request / per access)


Annotations
@Alternative
@Specializes
@Default


Qualifiers are optional.  Named("String") can be used.
