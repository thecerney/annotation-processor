package cerney.annotation.processor;


import cerney.annotation.custom.GetterSetter;
import com.sun.source.tree.Tree;
import com.sun.tools.javac.api.JavacTrees;
import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.tree.TreeTranslator;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.ListBuffer;
import com.sun.tools.javac.util.Name;
import com.sun.tools.javac.util.Names;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import java.util.Set;

@SupportedAnnotationTypes("cerney.annotation.custom.GetterSetter")
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class GetterSetterAnnotationProcessor extends AbstractProcessor {
    private Messager messager;
    private JavacTrees javacTrees;
    private TreeMaker treeMaker;
    private Names names;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        this.messager = processingEnv.getMessager();
        this.javacTrees = JavacTrees.instance(processingEnv);

        Context context = ((JavacProcessingEnvironment)processingEnv).getContext();
        this.treeMaker = TreeMaker.instance(context);
        this.names = Names.instance(context);
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        Set<? extends Element> elementsAnnotatedWith = roundEnv.getElementsAnnotatedWith(GetterSetter.class);
        elementsAnnotatedWith.forEach(element -> {
            // Obtain the AST structure of the class
            JCTree tree = javacTrees.getTree(element);

            // Traverse the class and modify the class
            tree.accept(new TreeTranslator() {
                @Override
                public void visitClassDef(JCTree.JCClassDecl jcClassDecl) {
                    List<JCTree.JCVariableDecl> jcVariableDeclList = List.nil();
                    // Find all variables in AST
                    for (JCTree jcTree : jcClassDecl.defs) {
                        if (jcTree.getKind().equals(Tree.Kind.VARIABLE)) {
                            JCTree.JCVariableDecl jcVariableDecl = (JCTree.JCVariableDecl) jcTree;
                            jcVariableDeclList = jcVariableDeclList.append(jcVariableDecl);
                        }
                    }

                    // Perform operations on the generation method for variables
                    for (JCTree.JCVariableDecl jcVariableDecl : jcVariableDeclList) {
                        messager.printMessage(Diagnostic.Kind.NOTE, jcVariableDecl.getName() + " has been processed");
//                        jcClassDecl.defs = jcClassDecl.defs.prepend(makeSetterMethodDecl(jcVariableDecl));

                        jcClassDecl.defs = jcClassDecl.defs.prepend(makeGetterMethodDecl(jcVariableDecl));
                    }


                    // Generate a returned object
                    JCTree.JCExpression methodType = treeMaker.Type(new Type.JCVoidType());

                    return treeMaker.MethodDef(treeMaker.Modifiers(Flags.PUBLIC), getNewSetterMethodName(jcVariableDecl.getName()), methodType, List.nil(), parameters, List.nil(), block, null);
                }

                /**
                 * Generate getter method
                 *
                 * @param jcVariableDecl
                 * @return
                 */
                private JCTree.JCMethodDecl makeGetterMethodDecl(JCTree.JCVariableDecl jcVariableDecl) {
                    ListBuffer<JCTree.JCStatement> statements = new ListBuffer<>();
                    // Generate an expression
                    JCTree.JCReturn aReturn = treeMaker.Return(treeMaker.Ident(jcVariableDecl.getName()));
                    statements.append(aReturn);
                    JCTree.JCBlock block = treeMaker.Block(0, statements.toList());
                    // No input parameter
                    // Generate a returned object
                    JCTree.JCExpression returnType = treeMaker.Type(jcVariableDecl.getType().type);
                    return treeMaker.MethodDef(treeMaker.Modifiers(Flags.PUBLIC), getNewGetterMethodName(jcVariableDecl.getName()), returnType, List.nil(), List.nil(), List.nil(), block, null);
                }

                /**
                 * Concatenate the Setter method name string
                 *
                 * @param name
                 * @return
                 */
                private Name getNewSetterMethodName(Name name) {
                    String s = name.toString();
                    return names.fromString("set" + s.substring(0, 1).toUpperCase() + s.substring(1, name.length()));
                }

                /**
                 * Concatenate the Getter method name string
                 *
                 * @param name
                 * @return
                 */
                private Name getNewGetterMethodName(Name name) {
                    String s = name.toString();
                    return names.fromString("get" + s.substring(0, 1).toUpperCase() + s.substring(1, name.length()));
                }

                /**
                 * Generate an expression
                 *
                 * @param lhs
                 * @param rhs
                 * @return
                 */
                private JCTree.JCExpressionStatement makeAssignment(JCTree.JCExpression lhs, JCTree.JCExpression rhs) {
                    return treeMaker.Exec(
                        treeMaker.Assign(lhs, rhs)
                    );
                }
            }
        }
    }
}
