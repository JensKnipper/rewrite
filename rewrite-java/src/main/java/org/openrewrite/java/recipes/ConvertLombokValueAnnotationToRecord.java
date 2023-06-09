package org.openrewrite.java.recipes;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.TreeVisitingPrinter;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Statement;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ConvertLombokValueAnnotationToRecord extends Recipe {
    @Override
    public String getDisplayName() {
        return "Convert classes to records where possible";
    }

    @Override
    public String getDescription() {
        return "Convert classes to records where possible.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new ConvertLombokValueAnnotationToRecordVisitor();
    }

    private class ConvertLombokValueAnnotationToRecordVisitor extends JavaIsoVisitor<ExecutionContext> {
        /*
        - lombok @Value to record
            - delete @Value annotation
            - but keep all other annotations
            - delete value import if present
            - change class to record
            - define fields as parameter list in record
            - rename all getters accesses of instances of class
            - rename all getter methods in class
            - keep all other methods
        - ends with Dto or DTO - not relevant, should be included in the others
        - predefine package and convert all the dtos in there?
        - all with public fields without getters
          - change only when final?
        - private field with getters
          - no setter
        - equals, hashcode, toString, getters allowed - when there are other methods, do not convert
         */

        @Override
        public J.CompilationUnit visitCompilationUnit(J.CompilationUnit cu, ExecutionContext executionContext) {
            System.out.println(TreeVisitingPrinter.printTree(getCursor()));

            List<J.Import> imports = new ArrayList<>(cu.getImports());
            boolean lombokValueImported = imports.removeIf(it -> it.getTypeName().equals("lombok.Value"));

            List<J.ClassDeclaration> classDeclarations = new ArrayList<>(cu
                    .getClasses())
                    .stream()
                    .map(it -> changeClassDeclaration(it, lombokValueImported, executionContext))
                    .collect(Collectors.toList());

            J.CompilationUnit newCompilationUnit = cu.withImports(imports).withClasses(classDeclarations);
            return super.visitCompilationUnit(newCompilationUnit, executionContext);
        }

        private J.ClassDeclaration changeClassDeclaration(J.ClassDeclaration classDecl, boolean lombokValueImported, ExecutionContext executionContext) {
            // TODO only for classes or also for interfaces, enums, annotation?
            if (!classDecl.getKind().equals(J.ClassDeclaration.Kind.Type.Class)) {
                return classDecl;
            }

            List<J.Annotation> annotations = new ArrayList<>(classDecl.getLeadingAnnotations());
            boolean isValue = annotations
                    .removeIf(it -> (it.getSimpleName().equals("Value") && lombokValueImported)
                            || it.getSimpleName().equals("lombok.Value"));
            if (!isValue) {
                return classDecl;
            }

            List<J.VariableDeclarations> fields = new ArrayList<>(classDecl.getBody().getStatements())
                    .stream()
                    .filter(it -> it instanceof J.VariableDeclarations)
                    .map(it -> (J.VariableDeclarations) it)
                    //TODO what about other modifiers?
                    .filter(it ->
                            it.getModifiers().stream().map(J.Modifier::getType).noneMatch(type -> type.equals(J.Modifier.Type.Static))
                    )
                    .collect(Collectors.toList());

            List<Statement> bodyStatements = new ArrayList<>(classDecl.getBody().getStatements());
            bodyStatements.removeAll(fields);

            return classDecl
                    .withLeadingAnnotations(annotations)
                    .withKind(J.ClassDeclaration.Kind.Type.Record)
                    .withBody(classDecl.getBody().withStatements(bodyStatements))
                    .withPrimaryConstructor(fields.stream().map(it -> (Statement) it).collect(Collectors.toList()));
        }
    }
}
