package pt.up.fe.comp2024.ast;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;

import static pt.up.fe.comp2024.ast.Kind.METHOD_DECL;

public class TypeUtils {

    private static final String INT_TYPE_NAME = "int";

    public static String getIntTypeName() {
        return INT_TYPE_NAME;
    }


    /**
     * Gets the {@link Type} of an arbitrary expression.
     *
     * @param expr
     * @param table
     * @return
     */
    public static Type getExprType(JmmNode expr, SymbolTable table) {
        // TODO: Simple implementation that needs to be expanded
        //System.out.print(" VisitVarRef2:" + expr  + "\n");
        var kind = Kind.fromString(expr.getKind());
        //System.out.print(" VisitVarRef3:" + kind  + "\n");
        Type type = switch (kind) {
            case BINARY_EXPR -> getBinExprType(expr);
            case VAR_REF_EXPR -> getVarExprType(expr, table);
            case INTEGER_LITERAL -> new Type(INT_TYPE_NAME, false);
            //case BOOLEAN_LITERAL -> new Type("boolean", false);
            default -> throw new UnsupportedOperationException("Can't compute type for expression kind '" + kind + "'");
        };
        //System.out.print(" VisitVarRef4:" + type  + "\n");
        return type;
    }

    private static Type getBinExprType(JmmNode binaryExpr) {
        // TODO: Simple implementation that needs to be expanded

        String operator = binaryExpr.get("op");

        return switch (operator) {
            case "+", "*" -> new Type(INT_TYPE_NAME, false);
            default ->
                    throw new RuntimeException("Unknown operator '" + operator + "' of expression '" + binaryExpr + "'");
        };
    }


    private static Type getVarExprType(JmmNode varRefExpr, SymbolTable table) {
        // TODO: Simple implementation that needs to be expanded
        String methodName = varRefExpr.getAncestor(METHOD_DECL).map(method -> method.get("name")).orElseThrow();
        Type retType = table.getReturnType(methodName);
        System.out.print(" E agora este?:" + retType + "\n");
        if(retType.getName().equals("boolean")) {
            return new Type("boolean", false);
        }
        else if(retType.getName().equals(INT_TYPE_NAME)) {
            return new Type(INT_TYPE_NAME, false);
        }
        else return new Type(INT_TYPE_NAME, false);
    }


    /**
     * @param sourceType
     * @param destinationType
     * @return true if sourceType can be assigned to destinationType
     */
    public static boolean areTypesAssignable(Type sourceType, Type destinationType) {
        // TODO: Simple implementation that needs to be expanded
        return sourceType.getName().equals(destinationType.getName());
    }
}
