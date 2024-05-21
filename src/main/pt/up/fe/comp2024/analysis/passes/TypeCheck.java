package pt.up.fe.comp2024.analysis.passes;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2024.analysis.AnalysisVisitor;
import pt.up.fe.comp2024.ast.Kind;
import pt.up.fe.comp2024.ast.NodeUtils;

import java.util.ArrayList;
import java.util.List;


public class TypeCheck extends AnalysisVisitor {

    private String method;
    private List<String> variables = new ArrayList<>();
    private List<String> imports = new ArrayList<>();
    private List<String> methods = new ArrayList<>();
    private List<String> params = new ArrayList<>();
    boolean isStatic;


    @Override
    public void buildVisitor(){
        addVisit(Kind.METHOD_DECL, this::visitMethodDecl);
        addVisit("WhileStmt", this::visitWhileStmt);
        addVisit("AssignStmt", this::visitAssignStmt);
        addVisit("IfElseStmt", this::visitIfElseStmt);
        addVisit("ArrayAssign", this::visitArrayAssign);
        addVisit("ReturnStmt", this::visitReturnStmt);
        addVisit("ExprStmt", this::visitStmt);
        addVisit("VarDecl", this::visitVarDecl);
        addVisit("ImportDecl", this::visitImportDecl);
        addVisit("ClassDecl", this::visitClassDecl);
        addVisit("Param", this::visitParam);
    }

    private Void visitParam(JmmNode jmmNode, SymbolTable table) {
        String paramName = jmmNode.get("name");

        if(!params.contains(paramName)) {
            params.add(paramName);
        }else{
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(jmmNode),
                    NodeUtils.getColumn(jmmNode),
                    "Parameter " + paramName + " already declared",
                    null)
            );
        }
        return null;
    }

    private Void visitClassDecl(JmmNode jmmNode, SymbolTable table) {
        String className = jmmNode.get("className");

        if(imports.contains(className)){

            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(jmmNode),
                    NodeUtils.getColumn(jmmNode),
                    "Class " + className + " already declared",
                    null)
            );
        }
        return null;
    }

    private Void visitImportDecl(JmmNode jmmNode, SymbolTable table) {
        String[] importNames = jmmNode.get("value").substring(1, jmmNode.get("value").length() - 1).split(",");
        String importName = importNames[importNames.length - 1];

        if(!imports.contains(importName)) {
            imports.add(importName);
        }else{
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(jmmNode),
                    NodeUtils.getColumn(jmmNode),
                    "Import " + importName + " already declared",
                    null)
            );
        }
        return null;
    }

    private Void visitVarDecl(JmmNode jmmNode, SymbolTable table) {
        String varName = jmmNode.get("name");

        if(!variables.contains(varName)) {
            variables.add(varName);
        }else{
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(jmmNode),
                    NodeUtils.getColumn(jmmNode),
                    "Variable " + varName + " already declared",
                    null)
            );
        }
        return null;
    }

    private Void visitStmt(JmmNode jmmNode, SymbolTable table){
        TypeGetter typeCheck = new TypeGetter(method, isStatic);

        typeCheck.visit(jmmNode.getJmmChild(0),table);

        typeCheck.reports.forEach(this::addReport);

        return null;
    }


    private Void visitReturnStmt(JmmNode jmmNode, SymbolTable table) {
        TypeGetter typeCheck = new TypeGetter(method, isStatic);

        Type a = typeCheck.visit(jmmNode.getJmmChild(0),table);

        typeCheck.reports.forEach(this::addReport);

        if (!a.equals(TypeGetter.ANY) && !a.equals(table.getReturnType(method))) {
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(jmmNode),
                    NodeUtils.getColumn(jmmNode),
                    "Return type must be the same as the method return type",
                    null)
            );
        }

        return null;
    }

    private Void visitArrayAssign(JmmNode jmmNode, SymbolTable table) {
        TypeGetter typeCheck = new TypeGetter(method, isStatic);

        var variable = table.getLocalVariables(method).stream().filter((v) -> v.getName().equals(jmmNode.get("var"))).findFirst();



        if (!variable.isPresent()){
            variable  = table.getFields().stream().filter((v) -> v.getName().equals(jmmNode.get("var"))).findFirst();
            if (!variable.isPresent()){
                variable = table.getParameters(method).stream().filter((v) -> v.getName().equals(jmmNode.get("var"))).findFirst();
                if (!variable.isPresent()){
                    addReport(Report.newError(
                            Stage.SEMANTIC,
                            NodeUtils.getLine(jmmNode),
                            NodeUtils.getColumn(jmmNode),
                            "Variable " + jmmNode.get("var") + " not found",
                            null)
                    );
                    return null;
                }
            }

        }

        if (!variable.get().getType().isArray()) {
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(jmmNode),
                    NodeUtils.getColumn(jmmNode),
                    "Variable must be an array",
                    null)
            );
        }

        return null;
    }


    private Void visitMethodDecl(JmmNode method, SymbolTable table) {
        this.method = method.get("name");
        this.variables = new ArrayList<>();
        this.params = new ArrayList<>();
        this.isStatic = method.get("isStatic").equals("true");
//        System.out.println(method.get("isStatic") + " " + method.get("name"));
        // if method is main check if the parameter is an array of strings

        var parameters = table.getParameters(this.method);



        if (this.method.equals("main")) {
//            System.out.println(method);
//            System.out.println(method.get("isStatic"));
            if (!method.get("isStatic").equals("true")) {
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        NodeUtils.getLine(method),
                        NodeUtils.getColumn(method),
                        "Main method must be static",
                        null)
                );
            }
            else if (method.getChildren("Param").size() != 1) {
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        NodeUtils.getLine(method),
                        NodeUtils.getColumn(method),
                        "Main method must have one parameter",
                        null)
                );
            } else if (!parameters.get(0).getName().equals("args")) {
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        NodeUtils.getLine(method),
                        NodeUtils.getColumn(method),
                        "Main method parameter must be named args",
                        null)
                );
            } else if (!parameters.get(0).getType().isArray() || !parameters.get(0).getType().getName().equals("String")){
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        NodeUtils.getLine(method),
                        NodeUtils.getColumn(method),
                        "Main method parameter must be an array of strings",
                        null)
                );
            }
        }else {
            if (method.get("isStatic").equals("true")) {
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        NodeUtils.getLine(method),
                        NodeUtils.getColumn(method),
                        "Method " + this.method + " must not be static",
                        null)
                );
            }
        }

        if(!methods.contains(this.method)) {
            methods.add(this.method);
        }else{
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(method),
                    NodeUtils.getColumn(method),
                    "Method " + this.method + " already declared",
                    null)
            );
        }


        return null;
    }



    private Void visitWhileStmt(JmmNode whileNode, SymbolTable table) {
        TypeGetter typeCheck = new TypeGetter(method, isStatic);

        Type a = typeCheck.visit(whileNode.getJmmChild(0), table);

        typeCheck.reports.forEach(this::addReport);

        if (!a.equals(TypeGetter.BOOL) && !a.equals(TypeGetter.ANY)) {
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(whileNode),
                    NodeUtils.getColumn(whileNode),
                    "While condition must be a boolean expression",
                    null)
            );

        }

        return null;
    }

    private Void visitAssignStmt(JmmNode assignStmt, SymbolTable table) {

        TypeGetter typeCheck = new TypeGetter(method, isStatic);

        var variable = table.getLocalVariables(method).stream().filter((v) -> v.getName().equals(assignStmt.get("var"))).findFirst();

        if (variable.isEmpty()){
            variable = table.getParameters(method).stream().filter((v) -> v.getName().equals(assignStmt.get("var"))).findFirst();
            if (variable.isEmpty()){
                variable  = table.getFields().stream().filter((v) -> v.getName().equals(assignStmt.get("var"))).findFirst();
                if (isStatic || variable.isEmpty()){
                    addReport(Report.newError(
                            Stage.SEMANTIC,
                            NodeUtils.getLine(assignStmt),
                            NodeUtils.getColumn(assignStmt),
                            "Variable " + assignStmt.get("var") + " not found",
                            null)
                    );
                    return null;
                }
            }

        }

        Type expr = typeCheck.visit(assignStmt.getJmmChild(0),table);
//        System.out.println(assignStmt.getJmmChild(0));
//        System.out.println(variable.get().getType().getName() + " tipo da variavel");
//        System.out.println(expr.getName() + " tipo da expressao");



        typeCheck.reports.forEach(this::addReport);

        var var1 = variable.get();

        if (!var1.getType().equals(expr) && !expr.equals(TypeGetter.ANY)) {
            if (isPrimitive(var1.getType())) {
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        NodeUtils.getLine(assignStmt),
                        NodeUtils.getColumn(assignStmt),
                        "Variable and expression must have the same type",
                        null)
                );
            }

            if (var1.getType().getName().equals(table.getClassName()) && table.getSuper() == null){
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        NodeUtils.getLine(assignStmt),
                        NodeUtils.getColumn(assignStmt),
                        "Variable and expression must have the same type",
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

        return null;
    }

    private boolean isPrimitive(Type type) {
        return type.getName().equals("int") || type.getName().equals("boolean") || type.getName().equals("int[]");
    }


    private Void visitIfElseStmt(JmmNode ifElseStmt, SymbolTable table) {
        TypeGetter typeCheck = new TypeGetter(method, isStatic);

        Type a = typeCheck.visit(ifElseStmt.getJmmChild(0), table);

        typeCheck.reports.forEach(this::addReport);

        if (!a.equals(TypeGetter.BOOL) && !a.equals(TypeGetter.ANY)) {
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(ifElseStmt),
                    NodeUtils.getColumn(ifElseStmt),
                    "If condition must be a boolean expression",
                    null)
            );
        }

        return null;
    }



}
