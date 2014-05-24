package de.jpaw.dp;

// scopes
annotation Singleton {}
// annotation PerThread {}  / not yet supported
annotation Dependent {}   // javax.enterprise.context 

interface Provider<T> {
	def T get();
}


// qualifiers
annotation Default {}
annotation Alternative {}
annotation Specializes {}
annotation Named {
	String value;
}

