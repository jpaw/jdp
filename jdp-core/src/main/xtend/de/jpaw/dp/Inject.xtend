package de.jpaw.dp;

import org.eclipse.xtend.lib.macro.AbstractFieldProcessor
import org.eclipse.xtend.lib.macro.Active
import org.eclipse.xtend.lib.macro.TransformationContext
import org.eclipse.xtend.lib.macro.declaration.MutableFieldDeclaration
import java.util.List

@Active(InjectProcessor) annotation Inject {}

class InjectProcessor extends AbstractFieldProcessor {

    def private static String skipGenericsTypeParameters(String it) {
        val idx = indexOf('<')
        return if (idx >= 0) substring(0, idx) else it
    }

    override doTransform(MutableFieldDeclaration fld, extension TransformationContext context) {
        val provider = Provider.newTypeReference.type
        val namedAnno = Named.newTypeReference.type
        val jdpClass = Jdp.newTypeReference

        val isAny = fld.findAnnotation(Any.newTypeReference.type) !== null
        val isOptional = fld.findAnnotation(Optional.newTypeReference.type) !== null

        val qualifier = fld.findAnnotation(namedAnno)?.getValue("value") as String
        val qualifierText = if (qualifier !== null) ''', "«qualifier»"'''
        val anyText = if (isAny) "All" else if (isOptional) "Optional" else "Required"

        if (isAny && isOptional) {
            fld.addError('''Cannot use @Any and @Optional on the same field''')
            return
        }

        val theType =
            if (isAny) {
                if (fld.type.type.qualifiedName != List.canonicalName) {
                    fld.addError('''field must be of type List when using @Any, found «fld.type.type.qualifiedName»''')
                    return
                 }
                 fld.type.actualTypeArguments.get(0)
            } else {
                 fld.type
            }

//        fld.docComment = '''
//            type args are «fld.type.actualTypeArguments.map[simpleName].join(':')» !
//            simple name is «fld.simpleName», type simple name is «fld.type.simpleName»
//            type is «fld.type.type.qualifiedName»'''
        fld.initializer = if (fld.type.type == provider)
            [ '''«toJavaCode(jdpClass)».getProvider(«toJavaCode(fld.type.actualTypeArguments.get(0))».class«qualifierText»)''']
        else
            [ '''«toJavaCode(jdpClass)».get«anyText»(«toJavaCode(theType).skipGenericsTypeParameters».class«qualifierText»)''']
        fld.final = true  // before xtend 2.7, this had issued an error as the initializer is not seen. Now it's fine!
    }
}
