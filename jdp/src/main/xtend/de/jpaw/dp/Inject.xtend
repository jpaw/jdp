package de.jpaw.dp;

import org.eclipse.xtend.lib.macro.AbstractFieldProcessor
import org.eclipse.xtend.lib.macro.Active
import org.eclipse.xtend.lib.macro.TransformationContext
import org.eclipse.xtend.lib.macro.declaration.MutableFieldDeclaration

@Active(typeof(InjectProcessor))
    annotation Inject {
}

class InjectProcessor extends AbstractFieldProcessor {

    override doTransform(MutableFieldDeclaration fld, extension TransformationContext context) {
    	val provider = Provider.newTypeReference.type
    	val namedAnno = Named.newTypeReference.type
    	val jdpClass = Jdp.newTypeReference
    	
    	val qualifier = fld.findAnnotation(namedAnno)?.getValue("value") as String
    	val qualifierText = if (qualifier != null) ''', "«qualifier»"''' 
    	fld.docComment = '''
    		type args are «fld.type.actualTypeArguments.map[simpleName].join(':')» !
    		simple name is «fld.simpleName», type simple name is «fld.type.simpleName»
    		type is «fld.type.type.qualifiedName»'''
    	fld.initializer = if (fld.type.type == provider)
    		[ '''«toJavaCode(jdpClass)».getProvider(«toJavaCode(fld.type.actualTypeArguments.get(0))».class«qualifierText»)''']
    	else
    		[ '''«toJavaCode(jdpClass)».get(«toJavaCode(fld.type)».class«qualifierText»)''']
    	// fld.final = true  // currently issues an error as the initializer is not seen
	}
}