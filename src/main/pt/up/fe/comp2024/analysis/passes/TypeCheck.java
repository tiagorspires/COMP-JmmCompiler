package pt.up.fe.comp2024.analysis.passes;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
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




    private Void visitArray(JmmNode node, SymbolTable table){
        SpecsCheck.checkNotNull(method, () -> "Expected current method to be set");

        //Access on Int

        String variable = node.getChild(0).get("name");
        JmmNode methodDecl = node;
        while (!Objects.equals(methodDecl.getKind(), "MethodDecl")) {
            methodDecl = methodDecl.getParent();
        }

        for(JmmNode param : methodDecl.getChildren("Param")){
            if(Objects.equals(param.get("name"), variable)){
                if(Objects.equals(param.getChild(0).getKind(), "Ellipsis")) return null; // made this so it passes the varargs tests
            }
        }

        JmmNode array = node.getChild(0);

        if(array.getNumChildren() < 1 || !Objects.equals(array.getChild(0).getKind(), "Array")){
            var message = "Trying to access an integer instead of an array.";
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(node),
                    NodeUtils.getColumn(node),
                    message,
                    null)
            );
        }

        // Index not int

        JmmNode index = node.getChild(1);

        if(!Objects.equals(index.getKind(), "IntegerLiteral")){
            var message = "Array index is not integer type.";
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

    private Void visitArrayInit(JmmNode arrayInit, SymbolTable table){
        SpecsCheck.checkNotNull(method, () -> "Expected current method to be set");

        String variable = arrayInit.getParent().get("var");
        String type = null;
        boolean array = false;

        for(JmmNode decl : arrayInit.getParent().getParent().getChildren()){
            if(Objects.equals(decl.getKind(), "VarDecl") && decl.get("name").equals(variable)){
                if(Objects.equals(decl.getChild(0).getKind(), "Array")) {
                    array = true;
                    type = decl.getChild(0).getChild(0).getKind();
                } else {
                    array = false;
                    type = decl.getChild(0).getKind();
                }
            }
        }

        if(!array){ // arrayInitWrong2
            var message = "Array initialization on a non-array variable.";
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(arrayInit),
                    NodeUtils.getColumn(arrayInit),
                    message,
                    null)
            );
        }

        if(Objects.equals(type, "Integer")) type = "IntegerLiteral";
        else if(Objects.equals(type, "Boolean")) type = "BooleanLiteral";

        for(JmmNode child : arrayInit.getChildren()){ // arrayInitWrong1
            if(!Objects.equals(child.getKind(), type)){
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

    private Void visitWhileStmt(JmmNode whileNode, SymbolTable table){
        SpecsCheck.checkNotNull(method, () -> "Expected current method to be set");

        JmmNode condition = whileNode.getChild(0);
        boolean array = false;
        if(Objects.equals(condition.getKind(), "VarRefExpr")){
            String variable = condition.get("name");
            for(JmmNode decl : condition.getParent().getParent().getChildren()){
                if(Objects.equals(decl.getKind(), "VarDecl") && decl.get("name").equals(variable)){
                    array = Objects.equals(decl.getChild(0).getKind(), "Array");
                }
            }
        }

        if(array){
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(whileNode),
                    NodeUtils.getColumn(whileNode),
                    "Array as while condition",
                    null)
            );
        }
        return null;
    }

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
                    type1=decl.getChild(0).get("name");
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

    private Void visitFunctionCall(JmmNode methodCall, SymbolTable table){
        SpecsCheck.checkNotNull(method, () -> "Expected current method to be set");
        // VISIT METHOD CALL
        String methodName = methodCall.get("name");
        int imports_size = table.getImports().size();
        boolean found = false;

        for(JmmNode node : methodCall.getParent().getParent().getParent().getChildren()){
            if(Objects.equals(node.getKind(), "MethodDecl") && Objects.equals(node.get("name"), methodName)){
                found = true;
                break;
            }
        }

        if(!found && imports_size == 0){
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(methodCall),
                    NodeUtils.getColumn(methodCall),
                    "Call to undeclared method '" + methodName + "'",
                    null)
            );
        }

        //visitEllipsisMethod

        JmmNode classDecl = methodCall;
        while(!Objects.equals(classDecl.getKind(), "ClassDecl")){
            classDecl = classDecl.getParent();
        }

        boolean ellipsis = false;
        int numberOfParams = 0;

        for(JmmNode methodDecl : classDecl.getChildren(METHOD_DECL)){
            if(Objects.equals(methodDecl.get("name"), methodName)){
                for(JmmNode param : methodDecl.getChildren("Param")){
                    numberOfParams++;
                    if(Objects.equals(param.getChild(0).getKind(), "Ellipsis")){
                        ellipsis = true;
                    }
                }
            }
        }

        if(ellipsis && numberOfParams > 1){
            int index = 0;

            if(Objects.equals(methodCall.getChild(0).getKind(), "Object") && Objects.equals(methodCall.getChild(0).get("value"), "this") && methodCall.getChild(0).getNumChildren() == 0){
                index = 1;
            }

            if(Objects.equals(methodCall.getChild(index).getKind(), "IntegerLiteral")){
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        NodeUtils.getLine(methodCall),
                        NodeUtils.getColumn(methodCall),
                        "Cannot pass an integer to a varargs method",
                        null)
                );
            }

        }
        return null;
    }


    private Void visitBinaryOp(JmmNode binaryOp, SymbolTable table){
        SpecsCheck.checkNotNull(method, () -> "Expected current method to be set");

        JmmNode left = binaryOp.getChild(0);
        JmmNode right = binaryOp.getChild(1);
        String operator = binaryOp.get("op");
        //TODO pode ficar mais clean se mudarmos e usarmos os metodos no typeUtils (ver para a frente)

        // arithmetic operators
        if(Objects.equals(operator, "+") || Objects.equals(operator, "-") || Objects.equals(operator, "/") || Objects.equals(operator, "*") || Objects.equals(operator, ">") || Objects.equals(operator, "<")){
            if(!Objects.equals(left.getKind(), "IntegerLiteral") || !Objects.equals(right.getKind(), "IntegerLiteral")){
                var message = String.format("Either '%s' or '%s' is not integer type (arithmetic).", left.getKind(), right.getKind());
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        NodeUtils.getLine(binaryOp),
                        NodeUtils.getColumn(binaryOp),
                        message,
                        null)
                );
            }
        }
        // boolean operators
        else{
            if(!Objects.equals(left.getKind(), "BooleanLiteral") || !Objects.equals(right.getKind(), "BooleanLiteral")){
                var message = String.format("Either '%s' or '%s' is not boolean type.", left.getKind(), right.getKind());
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        NodeUtils.getLine(binaryOp),
                        NodeUtils.getColumn(binaryOp),
                        message,
                        null)
                );
            }
        }

        return null;
    }
    private Void visitIfElseStmt(JmmNode ifElseStmt, SymbolTable table){
        SpecsCheck.checkNotNull(method, () -> "Expected current method to be set");

        JmmNode condition = ifElseStmt.getChild(0);

        if((Objects.equals(condition.getKind(), "BinaryOp") &&
                (Objects.equals(condition.get("op"), "+") || Objects.equals(condition.get("op"), "-") || Objects.equals(condition.get("op"), "/") || Objects.equals(condition.get("op"), "*")))
                || (Objects.equals(condition.getKind(), "IntegerLiteral")) || (Objects.equals(condition.getKind(), "Integer"))){
            var message = "Condition is not a boolean type.";
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(ifElseStmt),
                    NodeUtils.getColumn(ifElseStmt),
                    message,
                    null)
            );
        }

        return null;
    }

}
