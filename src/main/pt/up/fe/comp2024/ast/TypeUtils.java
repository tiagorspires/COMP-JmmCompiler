package pt.up.fe.comp2024.ast;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp2024.optimization.OptUtils;

import static pt.up.fe.comp2024.ast.Kind.METHOD_DECL;
import static pt.up.fe.comp2024.ast.Kind.NEW_CLASS;

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
            case BINARY_OP -> getBinExprType(expr);
            case VAR_REF_EXPR -> getVarExprType(expr, table);
            case INTEGER_LITERAL -> new Type(INT_TYPE_NAME, false);
            case BOOLEAN_LITERAL -> new Type("boolean", false);
            case NEW_CLASS -> new Type(expr.get("name"), false);
            case FUNCTION_CALL -> getVarExprType(expr, table);
            case NEGATION -> getVarExprType(expr, table);
            case NEW_ARRAY -> new Type(INT_TYPE_NAME, true);
            case ARRAY_ACCESS -> new Type("int", true);
            case ARRAY_INIT -> new Type("int", true);
            case LENGTH -> new Type("int", false);
            default -> throw new UnsupportedOperationException("Can't compute type for expression kind '" + kind + "'");
        };
        //System.out.print(" VisitVarRef4:" + type  + "\n");
        return type;
    }

    private static Type getBinExprType(JmmNode binaryExpr) {
        // TODO: Simple implementation that needs to be expanded
        //System.out.print(" Binary expr:"+binaryExpr+"\n");
        String operator = binaryExpr.get("op");

        return switch (operator) {
            case "+", "*", "-", "/" -> new Type(INT_TYPE_NAME, false);
            case "<", ">", "&&", "||" -> new Type("boolean", false);
            default ->
                    throw new RuntimeException("Unknown operator '" + operator + "' of expression '" + binaryExpr + "'");
        };
    }


    private static Type getVarExprType(JmmNode varRefExpr, SymbolTable table) {
//        // TODO: Simple implementation that needs to be expanded
//        String methodName = varRefExpr.getAncestor(METHOD_DECL).map(method -> method.get("name")).orElseThrow();
////        System.out.println(table.getParameters(methodName).contains(varRefExpr.get("name")));
//        Type retType = table.getReturnType(methodName);
//        System.out.println(" ola?:"+retType);
          System.out.print(" E agora este?:" + varRefExpr + "\n");
//        if(retType.getName().equals("boolean")) {
//            return new Type("boolean", false);
//        }
//        else if(retType.getName().equals(INT_TYPE_NAME)) {
//            return new Type(INT_TYPE_NAME, false);
//        }
//        else return new Type(INT_TYPE_NAME, false);
        String methodName = varRefExpr.getAncestor(METHOD_DECL).map(method -> method.get("name")).orElseThrow();
        Type retType = table.getReturnType(methodName);

        //System.out.println(" l:"+varRefExpr.getKind().equals("Negation"));
        if(varRefExpr.getKind().equals("Negation")) {
            return new Type("boolean", false);
        }


        //System.out.println(" ola?:"+retType);
        for(int i = 0; i<table.getParameters(methodName).size(); i++) {
            if(table.getParameters(methodName).get(i).getName().equals(varRefExpr.get("name"))) {
                //System.out.println(" parametro:"+table.getParameters(methodName).get(i).getType().getName());
                retType = table.getParameters(methodName).get(i).getType();
            }
        }

        for(int i = 0; i<table.getLocalVariables(methodName).size(); i++) {
            if(table.getLocalVariables(methodName).get(i).getName().equals(varRefExpr.get("name"))) {
//                System.out.println(" parametro:"+table.getLocalVariables(methodName).get(i).getType());
                retType = table.getLocalVariables(methodName).get(i).getType();
            }
        }


        if(retType.isArray()) {
            return new Type(retType.getName(), true);
        }
        else if(retType.getName().equals("boolean")) {
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
