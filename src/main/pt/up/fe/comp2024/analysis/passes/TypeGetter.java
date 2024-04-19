package pt.up.fe.comp2024.analysis.passes;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2024.ast.NodeUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class TypeGetter extends AJmmVisitor<SymbolTable, Type> {

    public List<Report> reports = new ArrayList<>();
    public final static Type INT = new Type("int",false);
    public final static Type BOOL = new Type("boolean",false);
    public final static Type ANY = new Type("any", false);
    private final String methodName;


    public TypeGetter(String methodName) {
        this.methodName = methodName;
    }


    protected void buildVisitor() {
        addVisit("Paren", (n,s) -> visit(n.getJmmChild(0)));
        addVisit("ArrayInit", this::visitArrayInit);
        addVisit("ArrayAccess", this::visitArrayAccess);
        addVisit("Length", this::visitLength);
        addVisit("FunctionCall", this::visitFunctionCall);
        addVisit("Object", this::visitObject);
        addVisit("Negation", this::visitNegation);
        addVisit("NewArray", this::visitNewArray);
        addVisit("NewClass", this::visitNewClass);
        addVisit("BinaryOp", this::visitBinaryOp);
        addVisit("IntegerLiteral", this::visitIntegerLiteral);
        addVisit("BooleanLiteral", this::visitBooleanLiteral);
        addVisit("VarRefExpr", this::visitVarRefExpr);

    }



    private Type visitArrayAccess(JmmNode jmmNode, SymbolTable table) {
        var arrayVar  = visit(jmmNode.getJmmChild(0),table);
        var index = visit(jmmNode.getJmmChild(1),table);

        if (!index.equals(INT)){
            var message = "Array index must be an integer";
            reports.add(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(jmmNode),
                    NodeUtils.getColumn(jmmNode),
                    message,
                    null
                    ));
        }

        if (!arrayVar.isArray()){
            var message = "Variable is not an array";
            reports.add(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(jmmNode),
                    NodeUtils.getColumn(jmmNode),
                    message,
                    null
            ));
        }

        return new Type(arrayVar.getName(),false);
    }

    private Type visitArrayInit(JmmNode jmmNode, SymbolTable table) {
        var firstType = visit(jmmNode.getJmmChild(0),table);
        for (JmmNode node: jmmNode.getChildren()){
            var type = visit(node,table);
            if (!type.equals(firstType)){
                var message = "Array elements must be of the same type";
                reports.add(Report.newError(
                        Stage.SEMANTIC,
                        NodeUtils.getLine(jmmNode),
                        NodeUtils.getColumn(jmmNode),
                        message,
                        null
                ));
                return type;
            }
        }
        return new Type(firstType.getName(),true);
    }

    private Type visitNewArray(JmmNode jmmNode, SymbolTable table) {
        Type type = visit(jmmNode.getJmmChild(0),table);

        if (!type.equals(INT)){
            var message = "Array size must be an integer";
            reports.add(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(jmmNode),
                    NodeUtils.getColumn(jmmNode),
                    message,
                    null
            ));
        }
        return type;
    }

    private Type visitNegation(JmmNode jmmNode, SymbolTable table) {
        if(!visit(jmmNode.getJmmChild(0),table).equals(BOOL)){
            var message = "Negation must be applied to a boolean";
            reports.add(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(jmmNode),
                    NodeUtils.getColumn(jmmNode),
                    message,
                    null
            ));
        }
        return new Type("bool",false);
    }

    private Type visitObject(JmmNode jmmNode, SymbolTable table) {
        return new Type(table.getClassName(),false);
    }

    private Type visitVarRefExpr(JmmNode jmmNode, SymbolTable table) {
        var varName = jmmNode.get("name");
        var a = table.getLocalVariables(methodName).stream().filter((x) -> x.getName().equals(varName)).findFirst();

        if (a.isPresent()){
            return a.get().getType();
        }

        a = table.getFields().stream().filter((x) -> x.getName().equals(varName)).findFirst();

        if (a.isPresent()){
            return a.get().getType();
        }

        a = table.getParameters(methodName).stream().filter((x) -> x.getName().equals(varName)).findFirst();

        if (a.isPresent()){
            return a.get().getType();
        }

        Optional<String> b = table.getImports().stream().filter((x) -> x.equals(varName)).findFirst();

        if (b.isPresent()){
            return new Type(b.get(),false);
        }

        String message = "Variable " + varName + " not found";
        reports.add(Report.newError(
                Stage.SEMANTIC,
                NodeUtils.getLine(jmmNode),
                NodeUtils.getColumn(jmmNode),
                message,
                null
        ));
        return ANY;
    }

    private Type visitBooleanLiteral(JmmNode jmmNode, SymbolTable table) {
        return BOOL;
    }

    private Type visitIntegerLiteral(JmmNode jmmNode, SymbolTable table) {
        return INT;
    }

    private Type visitBinaryOp(JmmNode jmmNode, SymbolTable table) {
        var left = visit(jmmNode.getJmmChild(0),table);
        var right = visit(jmmNode.getJmmChild(1),table);
        var op = jmmNode.get("op");

        if (left.equals(BOOL) && right.equals(BOOL) && (op.equals("||") || op.equals("&&"))){
            return BOOL;
        }

        if (left.equals(INT) && right.equals(INT) ){
            if ((op.equals("+") || op.equals("-") || op.equals("*") || op.equals("/") || op.equals("%"))){
                return INT;
            }else {
                return BOOL;
            }
        }

        var message = "Binary operation " + op + " not valid for types " + left.getName() + " and " + right.getName();
        reports.add(Report.newError(
                Stage.SEMANTIC,
                NodeUtils.getLine(jmmNode),
                NodeUtils.getColumn(jmmNode),
                message,
                null
        ));
        return ANY;
    }

    private Type visitNewClass(JmmNode jmmNode, SymbolTable table) {
        return new Type(jmmNode.get("name"), false);
    }

    private Type visitFunctionCall(JmmNode jmmNode, SymbolTable table) {
        var func = visit(jmmNode.getJmmChild(0), table);

        if (func.getName().equals(table.getClassName()) && table.getSuper() == null) {
            if (!table.getMethods().contains(jmmNode.get("name"))){
                var message = "Method " + methodName + " not found";
                reports.add(Report.newError(
                        Stage.SEMANTIC,
                        NodeUtils.getLine(jmmNode),
                        NodeUtils.getColumn(jmmNode),

                        message,
                        null
                ));
                return ANY;
            }

            var variables = table.getParameters(jmmNode.get("name"));

            if (!variables.isEmpty() && variables.get(0).getType().equals(new Type("int",true)) && jmmNode.getNumChildren() != variables.size() + 1){
                if (jmmNode.getNumChildren() == 2){
                    if (!visit(jmmNode.getJmmChild(1)).equals(new Type("int", true))){
                        reports.add(Report.newError(
                                Stage.SEMANTIC,
                                NodeUtils.getLine(jmmNode),
                                NodeUtils.getColumn(jmmNode),
                                "Argument 1 of method " + jmmNode.get("name") + " is of the wrong type",
                                null
                        ));
                    }
                }else {
                    for (int i = 1; i < jmmNode.getNumChildren(); i++) {
                        System.out.println(visit(jmmNode.getJmmChild(i), table));
                        if (!visit(jmmNode.getJmmChild(i), table).equals(INT)) {
                            var message = "Argument " + i + " of method " + jmmNode.get("name") + " is of the wrong type";
                            reports.add(Report.newError(
                                    Stage.SEMANTIC,
                                    NodeUtils.getLine(jmmNode),
                                    NodeUtils.getColumn(jmmNode),
                                    message,
                                    null
                            ));
                            return table.getReturnType(jmmNode.get("name"));
                        }
                    }
                }
            } else {
                System.out.println("here1");
                for (int i = 1; i < jmmNode.getNumChildren(); i++) {
                    if (!visit(jmmNode.getJmmChild(i), table).equals(variables.get(i - 1))) {
                        var message = "Argument " + i + " of method " + jmmNode.get("name") + " is of the wrong type";
                        reports.add(Report.newError(
                                Stage.SEMANTIC,
                                NodeUtils.getLine(jmmNode),
                                NodeUtils.getColumn(jmmNode),
                                message,
                                null
                        ));
                        return table.getReturnType(jmmNode.get("name"));
                    }
                }
            }

            return table.getReturnType(jmmNode.get("name"));

        }
        return ANY;
    }

    private Type visitLength(JmmNode jmmNode, SymbolTable table) {
        var b = visit(jmmNode.getJmmChild(0),table);

        if (!b.isArray()){
            var message = "Variable is not an array";
            reports.add(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(jmmNode),
                    NodeUtils.getColumn(jmmNode),
                    message,
                    null
            ));
        }

        return INT;
    }



}
