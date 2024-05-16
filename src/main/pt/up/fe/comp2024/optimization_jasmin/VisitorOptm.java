package pt.up.fe.comp2024.optimization_jasmin;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ast.JmmNodeImpl;

import java.util.Map;
import java.util.Optional;
import java.util.function.BiPredicate;
import java.util.function.BinaryOperator;

public class VisitorOptm extends AJmmVisitor<Integer, Optional<String>> {


    SymbolTable symbolTable;



    Map<String, String> varValues;
    public VisitorOptm(SymbolTable symbolTable) {
        super();
        varValues = new java.util.HashMap<>();
        this.symbolTable = symbolTable;
    }

    @Override
    protected void buildVisitor() {
        addVisit("IfElseStmt",this::dealWithIfElseStmt);
        addVisit("BinaryOp",this::dealWithBinaryOp);
        addVisit("IntegerLiteral", this::dealWithIntegerLiteral);
        addVisit("BooleanLiteral", this::dealWithBooleanLiteral);
        addVisit( "VarRefExpr", this::dealWithVarRefExpr);
        addVisit("AssignStmt", this::dealWithAssignStmt);
    }

    private Optional<String> dealWithIntegerLiteral(JmmNode jmmNode, Integer integer) {
        return Optional.of(jmmNode.get("value"));
    }

    private Optional<String> dealWithBooleanLiteral(JmmNode jmmNode, Integer integer) {
        return Optional.of(jmmNode.get("value"));
    }




    private Optional<String> dealWithAssignStmt(JmmNode node, Integer integer) {
        Optional<String> valueOpt = visit(node.getChildren().get(0), 0);
        String varName = node.get("var");

        if (valueOpt.isPresent()) {
            updateVariableAndRemoveNode(node, integer, valueOpt.get(), varName);
        } else {
            varValues.remove(varName);
        }

        return Optional.empty();
    }

    private void updateVariableAndRemoveNode(JmmNode node, Integer integer, String value, String varName) {
        varValues.put(varName, value);
        node.getParent().removeJmmChild(integer);
    }

    private Optional<String> dealWithVarRefExpr(JmmNode node, Integer integer) {
        String name = node.get("value");
        return varValues.containsKey(name) ? processIdentifier(node, integer, name) : Optional.empty();
    }

    private Optional<String> processIdentifier(JmmNode node, Integer integer, String name) {

        String value = varValues.get(name);
        JmmNode child = createChildNode(value);
        child.put("value", value);
        node.getParent().setChild(child, integer);

        return Optional.of(value);
    }

    private JmmNode createChildNode(String value) {
        return isIntegerLiteral(value) ? new JmmNodeImpl("IntegerLiteral") : new JmmNodeImpl("BooleanLiteral");
    }

    private boolean isIntegerLiteral(String value) {
        try {
            Integer.parseInt(value);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }


    private Optional<String> dealWithIfElseStmt(JmmNode node, Integer integer) {
        Optional<String> valueOpt = visit(node.getChildren().get(0), 0);

        valueOpt.ifPresent(s -> handleIfCondition(node, integer, s));

        return Optional.empty();
    }

    private void handleIfCondition(JmmNode node, Integer integer, String value) {
        JmmNode parent = node.getParent();

        if (value.equals("true")) {
            executeTrueBranch(node, parent, integer);
        } else if (node.getNumChildren() == 3) {
            executeFalseBranch(node, parent, integer);
        }
    }

    private void executeTrueBranch(JmmNode node, JmmNode parent, Integer integer) {
        parent.setChild(node.getChildren().get(1), integer);
    }

    private void executeFalseBranch(JmmNode node, JmmNode parent, Integer integer) {
        parent.setChild(node.getChildren().get(2), integer);
    }



    private Optional<String> dealWithBinaryOp(JmmNode node, int integer) {
        Optional<String> leftOpt = visit(node.getChildren().get(0), 0);
        Optional<String> rightOpt = visit(node.getChildren().get(1), 1);

        if (leftOpt.isPresent() && rightOpt.isPresent()) {
            String left = leftOpt.get();
            String right = rightOpt.get();
            String op = node.get("op");
            JmmNode child = null;

            if ("ADD".equals(op)) {
                child = createIntegerNode(left, right, Integer::sum);
            } else if ("SUB".equals(op)) {
                child = createIntegerNode(left, right, (a, b) -> a - b);
            } else if ("MUL".equals(op)) {
                child = createIntegerNode(left, right, (a, b) -> a * b);
            } else if ("DIV".equals(op)) {
                child = createIntegerNode(left, right, (a, b) -> a / b);
            } else if ("LESS".equals(op)) {
                child = createBooleanNode(left, right, (BiPredicate<Integer, Integer>) (a, b) -> a < b);
            } else if ("GREATER".equals(op)) {
                child = createBooleanNode(left, right, (BiPredicate<Integer, Integer>) (a, b) -> a > b);
            } else if ("EQ".equals(op)) {
                child = createBooleanNode(left, right, Integer::equals);
            } else if ("AND".equals(op)) {
                child = createBooleanNode(left, right, Boolean::logicalAnd);
            } else if ("OR".equals(op)) {
                child = createBooleanNode(left, right, Boolean::logicalOr);
            } else {
                throw new RuntimeException("Unknown operator: " + op);
            }

            updateParentNode(node, child, integer);
            return Optional.of(child.get("value"));
        }
        return Optional.empty();
    }

    private JmmNode createIntegerNode(String left, String right, BinaryOperator<Integer> op) {
        int leftVal = Integer.parseInt(left);
        int rightVal = Integer.parseInt(right);
        int result = op.apply(leftVal, rightVal);

        JmmNode child = new JmmNodeImpl("IntegerLiteral");
        child.put("value", String.valueOf(result));
        return child;
    }

    private JmmNode createBooleanNode(String left, String right, BiPredicate<Integer, Integer> op) {
        int leftVal = Integer.parseInt(left);
        int rightVal = Integer.parseInt(right);
        boolean result = op.test(leftVal, rightVal);

        JmmNode child = new JmmNodeImpl("BooleanLiteral");
        child.put("value", String.valueOf(result));
        return child;
    }

    private JmmNode createBooleanNode(String left, String right, BinaryOperator<Boolean> op) {
        boolean leftVal = Boolean.parseBoolean(left);
        boolean rightVal = Boolean.parseBoolean(right);
        boolean result = op.apply(leftVal, rightVal);

        JmmNode child = new JmmNodeImpl("BooleanLiteral");
        child.put("value", String.valueOf(result));
        return child;
    }

    private void updateParentNode(JmmNode node, JmmNode child, int s) {
        JmmNode parent = node.getParent();
        node.removeJmmChild(0);
        node.removeJmmChild(1);
        parent.setChild(child, s);
    }


}
