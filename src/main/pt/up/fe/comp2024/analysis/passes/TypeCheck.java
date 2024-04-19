package pt.up.fe.comp2024.analysis.passes;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2024.analysis.AnalysisVisitor;
import pt.up.fe.comp2024.ast.Kind;
import pt.up.fe.comp2024.ast.NodeUtils;
import pt.up.fe.specs.util.SpecsCheck;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static pt.up.fe.comp2024.ast.Kind.FUNCTION_CALL;
import static pt.up.fe.comp2024.ast.Kind.METHOD_DECL;

public class TypeCheck extends AnalysisVisitor {

    private String method;

    @Override
    public void buildVisitor(){
        addVisit(Kind.METHOD_DECL, this::visitMethodDecl);
        addVisit("ArrayAccess", this::visitArray);
        addVisit("ArrayInit", this::visitArrayInit);
        addVisit("WhileStmt", this::visitWhileStmt);
        addVisit("AssignStmt", this::visitAssignStmt);
        addVisit("FunctionCall", this::visitFunctionCall);
        addVisit("BinaryOp", this::visitBinaryOp);
        addVisit("IfElseStmt", this::visitIfElseStmt);
    }

    private Void visitMethodDecl(JmmNode method, SymbolTable table) {
        this.method = method.get("name");
        return null;
    }




    private Void visitArray(JmmNode node, SymbolTable table) {
        SpecsCheck.checkNotNull(method, () -> "Expected current method to be set");

        String variableName = node.getChild(0).get("name");
        String extractedName = extractVariableName(node, variableName, table);

        if (extractedName == null) {
            return null; // Skip analysis if extraction failed
        }

        JmmNode array = node.getChild(0);

        if (array.getNumChildren() < 1 || !Objects.equals(array.getChild(0).getKind(), "Array")) {
            var message = "Trying to access an integer instead of an array.";
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(node),
                    NodeUtils.getColumn(node),
                    message,
                    null)
            );
        }

        // Check if the index is not an integer
        JmmNode index = node.getChild(1);
        if (!Objects.equals(index.getKind(), "IntegerLiteral")) {
            var message = "Array index is not of integer type.";
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(node),
                    NodeUtils.getColumn(node),
                    message,
                    null)
            );
        }

        return null;
    }

    private String extractVariableName(JmmNode arrayNode, String variableName, SymbolTable symbolTable) {
        JmmNode methodDeclaration = findMethodDeclaration(arrayNode);
        if (methodDeclaration == null) {
            return null; // Cannot proceed with analysis, method declaration not found
        }

        for (JmmNode parameter : methodDeclaration.getChildren("Param")) {
            if (Objects.equals(parameter.get("name"), variableName)) {
                if (Objects.equals(parameter.getChild(0).getKind(), "Ellipsis")) {
                    return null; // Skip analysis if parameter is a varargs
                }
            }
        }

        return arrayNode.getChild(0).get("name");
    }

    private JmmNode findMethodDeclaration(JmmNode node) {
        while (node != null && !Objects.equals(node.getKind(), "MethodDecl")) {
            node = node.getParent();
        }
        return node;
    }


    private Void visitArrayInit(JmmNode arrayInit, SymbolTable table) {
        SpecsCheck.checkNotNull(method, () -> "Expected current method to be set");

        String variable = arrayInit.getParent().get("var");
        TypeAndArray typeAndArray = findTypeAndArray(variable, arrayInit.getParent().getParent());

        if (typeAndArray == null) {
            return null; // Unable to find variable declaration
        }

        String type = typeAndArray.getType();
        boolean array = typeAndArray.isArray();

        if (!array) { // arrayInitWrong2
            var message = "Array initialization on a non-array variable.";
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(arrayInit),
                    NodeUtils.getColumn(arrayInit),
                    message,
                    null)
            );
        }

        if (Objects.equals(type, "Integer")) type = "IntegerLiteral";
        else if (Objects.equals(type, "Boolean")) type = "BooleanLiteral";

        for (JmmNode child : arrayInit.getChildren()) { // arrayInitWrong1
            if (!Objects.equals(child.getKind(), type)) {
                var message = "Array initialization with wrong type";
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        NodeUtils.getLine(child),
                        NodeUtils.getColumn(child),
                        message,
                        null)
                );
            }
        }

        return null;
    }

    private static class TypeAndArray {
        private final String type;
        private final boolean array;

        public TypeAndArray(String type, boolean array) {
            this.type = type;
            this.array = array;
        }

        public String getType() {
            return type;
        }

        public boolean isArray() {
            return array;
        }
    }

    private TypeAndArray findTypeAndArray(String variable, JmmNode parentNode) {
        for (JmmNode decl : parentNode.getChildren()) {
            if (Objects.equals(decl.getKind(), "VarDecl") && decl.get("name").equals(variable)) {
                String type = null;
                boolean array = false;
                if (Objects.equals(decl.getChild(0).getKind(), "Array")) {
                    array = true;
                    type = decl.getChild(0).getChild(0).getKind();
                } else {
                    type = decl.getChild(0).getKind();
                }
                return new TypeAndArray(type, array);
            }
        }
        return null; // Variable declaration not found
    }


    //////////////////////////////////////

    private Void visitWhileStmt(JmmNode whileNode, SymbolTable table) {
        SpecsCheck.checkNotNull(method, () -> "Expected current method to be set");

        JmmNode condition = whileNode.getChild(0);

        if (isArrayCondition(condition, table)) {
            addArrayWhileConditionError(whileNode);
        }

        return null;
    }

    private boolean isArrayCondition(JmmNode condition, SymbolTable table) {
        if (!Objects.equals(condition.getKind(), "VarRefExpr")) {
            return false;
        }

        String variable = condition.get("name");

        for (JmmNode decl : condition.getParent().getParent().getChildren()) {
            if (Objects.equals(decl.getKind(), "VarDecl") && decl.get("name").equals(variable)) {
                return Objects.equals(decl.getChild(0).getKind(), "Array");
            }
        }

        return false;
    }

    private void addArrayWhileConditionError(JmmNode whileNode) {
        addReport(Report.newError(
                Stage.SEMANTIC,
                NodeUtils.getLine(whileNode),
                NodeUtils.getColumn(whileNode),
                "Array used as a condition in a while statement",
                null)
        );
    }


    /// Todo visitAssignStmt

    /*

    private Void visitAssignStmt(JmmNode assign, SymbolTable table){

        // assign int to bool
        SpecsCheck.checkNotNull(method, () -> "Expected current method to be set");

        String variable = assign.get("var");
        String type = null;
        for(JmmNode decl : assign.getParent().getChildren()){
            if(Objects.equals(decl.getKind(), "VarDecl") && decl.get("name").equals(variable)){
                type=decl.getChild(0).getKind();
            }
        }
        if(Objects.equals(type, "Boolean")){
            if(Objects.equals(assign.getChild(0).getKind(), "IntegerLiteral")){
                var message = "Assigning an integer to a boolean.";
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        NodeUtils.getLine(assign),
                        NodeUtils.getColumn(assign),
                        message,
                        null)
                );
            }
        }

        // object assigment
        if(Objects.equals(assign.getChild(0).getKind(), "VarRefExpr")){
            String type1 = null; // type of the variable being assigned
            String type2 = null; // type of the variable being assigned to

            for(JmmNode decl : assign.getParent().getChildren()){
                if(Objects.equals(decl.getKind(), "VarDecl") && decl.get("name").equals(assign.get("var"))){
                    type1=decl.getChild(0).get("value");
                }
                if(Objects.equals(decl.getKind(), "VarDecl") && decl.get("name").equals(assign.getChild(0).get("name"))){
                    type2=decl.getChild(0).get("name");
                }
            }

            if(!Objects.equals(type1, type2)){
                // check if type1 extends type2 or if both types are imported
                String extendClass = table.getSuper();
                List<String> imports = table.getImports();
                List<String> modifiedImports = new ArrayList<>();

                for (String importString : imports) {
                    String[] elements = importString.substring(1, importString.length() - 1).split(",");
                    for (String element : elements) {
                        modifiedImports.add(element.trim());
                    }
                }

                if(!(modifiedImports.contains(type1) && modifiedImports.contains(type2))){
                    if(Objects.equals(extendClass, null) || !Objects.equals(extendClass, type1)){
                        var message = "Assigning a variable of type "+type2+" to a variable of type "+type1+".";
                        addReport(Report.newError(
                                Stage.SEMANTIC,
                                NodeUtils.getLine(assign),
                                NodeUtils.getColumn(assign),
                                message,
                                null)
                        );
                    }
                }
            }
        }
        return null;
    }

    */

    private Void visitAssignStmt(JmmNode assignStmt, SymbolTable table) {
//        System.out.println(assignStmt);
        SpecsCheck.checkNotNull(method, () -> "Expected current method to be set");
//        System.out.println(assignStmt.getChild(0));

        if(assignStmt.getNumChildren() > 0 && (Objects.equals(assignStmt.getChild(0).getKind(), "IntegerLiteral"))) {
            String leftHandSide = assignStmt.get("var");
            String rightHandSide = assignStmt.getChild(0).get("value");
//            System.out.println(leftHandSide);
//            System.out.println(rightHandSide);
            var Type = new Type("Integer", false);


            String rightType = Type.getName();
            String leftType = getVariableType(leftHandSide, assignStmt.getParent().getChildren());
//            System.out.println(" rightType:"+rightType);
//            System.out.println(" leftType:"+leftType);

            if (leftType != null && rightType != null && !leftType.equals(rightType)) {
                List<String> imports = table.getImports();
                String extendsClass = table.getSuper();

                if(!(imports.contains(rightType) && imports.contains(leftType)) || (extendsClass != null && !Objects.equals(extendsClass, rightType) && !Objects.equals(extendsClass, leftType))) {
                    addReport(Report.newError(
                            Stage.SEMANTIC,
                            NodeUtils.getLine(assignStmt),
                            NodeUtils.getColumn(assignStmt),
                            "Type mismatch: cannot convert from " + rightType + " to " + leftType,
                            null)
                    );
                }
            }
        }

        if(assignStmt.getNumChildren() > 0 && (Objects.equals(assignStmt.getChild(0).getKind(), "BooleanLiteral"))) {
            String leftHandSide = assignStmt.get("var");
            String rightHandSide = assignStmt.getChild(0).get("value");
//            System.out.println(leftHandSide);
//            System.out.println(rightHandSide);
            var Type = new Type("Boolean", false);


            String rightType = Type.getName();
            String leftType = getVariableType(leftHandSide, assignStmt.getParent().getChildren());
//            System.out.println(" leftType:"+leftType);

            if (leftType != null && rightType != null && !leftType.equals(rightType)) {
                List<String> imports = table.getImports();
                String extendsClass = table.getSuper();

                if(!(imports.contains(rightType) && imports.contains(leftType)) || (extendsClass != null && !Objects.equals(extendsClass, rightType) && !Objects.equals(extendsClass, leftType))) {
                    addReport(Report.newError(
                            Stage.SEMANTIC,
                            NodeUtils.getLine(assignStmt),
                            NodeUtils.getColumn(assignStmt),
                            "Type mismatch: cannot convert from " + rightType + " to " + leftType,
                            null)
                    );
                }
            }
        }

        // check if the assigned node has a valid right hand side that is a variable reference
        if (assignStmt.getNumChildren() > 0 && (Objects.equals(assignStmt.getChild(0).getKind(), "VarRefExpr"))) {
            String leftHandSide = assignStmt.get("var");
            String rightHandSide = assignStmt.getChild(0).get("name");

            // get type from left side and right side
            String leftType = getVariableType(leftHandSide, assignStmt.getParent().getChildren());
            String rightType = getVariableType(rightHandSide, assignStmt.getParent().getChildren());
//            System.out.println(" tipoLeft:"+leftType);
//            System.out.println(" tipoRight:"+rightType);


            // check if the types are the same
            if (leftType != null && rightType != null && !leftType.equals(rightType)) {
                List<String> imports = table.getImports();
                String extendsClass = table.getSuper();
                List<String> modifiedImports = new ArrayList<>();
                boolean extend = false;

//                System.out.println(" super:"+extendsClass);
//                System.out.println(" imports:"+imports);
                for (String importString : imports) {
                    String[] elements = importString.substring(1, importString.length() - 1).split(",");
                    for (String element : elements) {
                        modifiedImports.add(element.trim());
                    }
                }
                for (String importString : modifiedImports) {
                    if(extendsClass != null && extendsClass.equals(importString)){
                        extend = true;
                    }
                }
//                System.out.println(" modified imports"+modifiedImports);
//                System.out.println(" does it extend?:"+extend);
//                System.out.println(" CONTEM?:"+imports.contains(rightType));
                if((!extend) || (modifiedImports.contains(rightType) && modifiedImports.contains(leftType))) {
                    if (!(modifiedImports.contains(rightType) && modifiedImports.contains(leftType)) || (extendsClass != null && !Objects.equals(extendsClass, rightType) && !Objects.equals(extendsClass, leftType))) {
                        addReport(Report.newError(
                                Stage.SEMANTIC,
                                NodeUtils.getLine(assignStmt),
                                NodeUtils.getColumn(assignStmt),
                                "Type mismatch: cannot convert from " + rightType + " to " + leftType,
                                null)
                        );
                    }
                }
            }

        }
        return null;
    }

    private String getVariableType(String variable, List<JmmNode> nodes) {
//        System.out.println(" variable:"+variable);
        for (JmmNode decl : nodes) {
//            System.out.println(" getVariableType:"+decl);
//            System.out.println(" getdeclParent:"+decl.getChildren());
            if (Objects.equals(decl.getKind(), "VarDecl") && decl.get("name").equals(variable)) {
//                System.out.println(decl.getChild(0));
                if(decl.getChild(0).toString().equals("Boolean")) {
                    return decl.getChild(0).toString();
                }
                if(decl.getChild(0).toString().equals("Integer")) {
                    return decl.getChild(0).toString();
                }
                return decl.getChild(0).get("name");
            }
//            if (Objects.equals(decl.getKind(), "VarDecl") && decl.get("name").equals(variable) && decl.getChild(0).equals("Boolean")) {
//                System.out.println(decl.getChild(0));
//                return decl.getChild(0).toString();
//            }
        }
        return null;
    }

    ////////////////////////////////////

    private Void visitFunctionCall(JmmNode methodCall, SymbolTable table) {
//        System.out.println(" methodCall:"+methodCall);
        //System.out.println(" methodCall name:"+methodCall.getChild(0).get("name"));
        SpecsCheck.checkNotNull(method, () -> "Expected current method to be set");

        String methodName = methodCall.get("name");
        int importsSize = table.getImports().size();

//        System.out.println(" methodName:"+methodName);
//        System.out.println(" imports:"+table.getImports());
//
//        List<String> imports = table.getImports();
//        List<String> modifiedImports = new ArrayList<>();
//        boolean imported = false;
//
//        for (String importString : imports) {
//            String[] elements = importString.substring(1, importString.length() - 1).split(",");
//            for (String element : elements) {
//                modifiedImports.add(element.trim());
//            }
//        }
//        for (String importString : modifiedImports) {
//            if(methodCall.getChild(0).get("name").equals(importString)){
//                imported = true;
//            }
//        }
//
//        System.out.println(" imported?:"+imported);
//
//        if((!imported) || (!isMethodDeclared(methodCall, methodName) && importsSize == 0)) {
//            addUndeclaredMethodError(methodCall, methodName);
//        }

        if (!isMethodDeclared(methodCall, methodName) && importsSize == 0) {
            addUndeclaredMethodError(methodCall, methodName);
        }


        checkVarargsParameter(methodCall);

        return null;
    }

    private boolean isMethodDeclared(JmmNode methodCall, String methodName) {
        for (JmmNode node : methodCall.getParent().getParent().getParent().getChildren()) {
            //System.out.println(" node:"+node);
            if (Objects.equals(node.getKind(), "MethodDecl") && Objects.equals(node.get("name"), methodName)) {
                return true;
            }
        }
        return false;
    }

    private void addUndeclaredMethodError(JmmNode methodCall, String methodName) {
        addReport(Report.newError(
                Stage.SEMANTIC,
                NodeUtils.getLine(methodCall),
                NodeUtils.getColumn(methodCall),
                "Call to undeclared method '" + methodName + "'",
                null)
        );
    }

    private void checkVarargsParameter(JmmNode methodCall) {
        boolean ellipsis = false;
        int numberOfParams = 0;
        JmmNode classDecl = getClassDeclaration(methodCall);

        for (JmmNode methodDecl : classDecl.getChildren(METHOD_DECL)) {
            if (Objects.equals(methodDecl.get("name"), methodCall.get("name"))) {
                for (JmmNode param : methodDecl.getChildren("Param")) {
                    numberOfParams++;
                    if (Objects.equals(param.getChild(0).getKind(), "Ellipsis")) {
                        ellipsis = true;
                    }
                }
            }
        }

        if (ellipsis && numberOfParams > 1) {
            int index = isThisObjectReference(methodCall) ? 1 : 0;

            if (Objects.equals(methodCall.getChild(index).getKind(), "IntegerLiteral")) {
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        NodeUtils.getLine(methodCall),
                        NodeUtils.getColumn(methodCall),
                        "Cannot pass an integer to a varargs method",
                        null)
                );
            }

            if (expr.getName().equals(table.getClassName()) && table.getSuper() == null){
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        NodeUtils.getLine(assignStmt),
                        NodeUtils.getColumn(assignStmt),
                        "Variable and expression must have the same type",
                        null)
                );
            }
        }
    }

    private JmmNode getClassDeclaration(JmmNode methodCall) {
        JmmNode classDecl = methodCall;
        while (!Objects.equals(classDecl.getKind(), "ClassDecl")) {
            classDecl = classDecl.getParent();
        }
        return classDecl;
    }

    private boolean isThisObjectReference(JmmNode methodCall) {
        return Objects.equals(methodCall.getChild(0).getKind(), "Object")
                && Objects.equals(methodCall.getChild(0).get("value"), "this")
                && methodCall.getChild(0).getNumChildren() == 0;
    }


    ///////////////////////////////////


    private Void visitBinaryOp(JmmNode binaryOp, SymbolTable table) {
        SpecsCheck.checkNotNull(method, () -> "Expected current method to be set");

        JmmNode left = binaryOp.getChild(0);
        JmmNode right = binaryOp.getChild(1);
//        System.out.println(" left:"+left.getKind());
//        System.out.println(" right:"+right.getKind());
        String operator = binaryOp.get("op");

        boolean isArithmeticOperator = Objects.equals(operator, "+") || Objects.equals(operator, "-") || Objects.equals(operator, "/") || Objects.equals(operator, "*") || Objects.equals(operator, ">") || Objects.equals(operator, "<");

        if(left.getKind().equals("FunctionCall") || right.getKind().equals("FunctionCall")) {
            return null;
        }

        if (isArithmeticOperator && (!isIntegerLiteral(left) || !isIntegerLiteral(right))) {
            addArithmeticTypeError(binaryOp, left, right);
        } else if (!isArithmeticOperator && (!isBooleanLiteral(left) || !isBooleanLiteral(right))) {
            addBooleanTypeError(binaryOp, left, right);
        }

        return null;
    }

    private boolean isIntegerLiteral(JmmNode node) {
        return Objects.equals(node.getKind(), "IntegerLiteral");
    }

    private boolean isBooleanLiteral(JmmNode node) {
        return Objects.equals(node.getKind(), "BooleanLiteral");
    }

    private void addArithmeticTypeError(JmmNode binaryOp, JmmNode left, JmmNode right) {
        var message = String.format("Either '%s' or '%s' is not integer type (arithmetic).", left.getKind(), right.getKind());
        addReport(Report.newError(
                Stage.SEMANTIC,
                NodeUtils.getLine(binaryOp),
                NodeUtils.getColumn(binaryOp),
                message,
                null)
        );
    }

    private void addBooleanTypeError(JmmNode binaryOp, JmmNode left, JmmNode right) {
        var message = String.format("Either '%s' or '%s' is not boolean type.", left.getKind(), right.getKind());
        addReport(Report.newError(
                Stage.SEMANTIC,
                NodeUtils.getLine(binaryOp),
                NodeUtils.getColumn(binaryOp),
                message,
                null)
        );
    }

    private Void visitIfElseStmt(JmmNode ifElseStmt, SymbolTable table) {
        SpecsCheck.checkNotNull(method, () -> "Expected current method to be set");

        JmmNode condition = ifElseStmt.getChild(0);

        if (!isBooleanCondition(condition)) {
            addNonBooleanConditionError(ifElseStmt);
        }

        return null;
    }

    private boolean isBooleanCondition(JmmNode condition) {
        return isBinaryBooleanOperation(condition) || isBooleanLiteral(condition);
    }

    private boolean isBinaryBooleanOperation(JmmNode condition) {
        String operator = condition.get("op");
        return Objects.equals(condition.getKind(), "BinaryOp") &&
                (Objects.equals(operator, ">") || Objects.equals(operator, "<") ||
                        Objects.equals(operator, "&&") || Objects.equals(operator, "||") ||
                        Objects.equals(operator, "==") || Objects.equals(operator, "!="));
    }


    private void addNonBooleanConditionError(JmmNode ifElseStmt) {
        var message = "Condition is not a boolean type.";
        addReport(Report.newError(
                Stage.SEMANTIC,
                NodeUtils.getLine(ifElseStmt),
                NodeUtils.getColumn(ifElseStmt),
                message,
                null)
        );
    }


}
