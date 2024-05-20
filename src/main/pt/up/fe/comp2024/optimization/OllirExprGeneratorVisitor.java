package pt.up.fe.comp2024.optimization;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ast.PreorderJmmVisitor;
import pt.up.fe.comp2024.ast.TypeUtils;

import java.util.ArrayList;
import java.util.List;

import static pt.up.fe.comp2024.ast.Kind.*;

/**
 * Generates OLLIR code from JmmNodes that are expressions.
 */
public class OllirExprGeneratorVisitor extends AJmmVisitor<Void, OllirExprResult> {

    private static final String SPACE = " ";
    private static final String ASSIGN = ":=";
    private final String END_STMT = ";\n";

    private final SymbolTable table;

    public OllirExprGeneratorVisitor(SymbolTable table) {
        this.table = table;
    }

    @Override
    protected void buildVisitor() {
        addVisit(VAR_REF_EXPR, this::visitVarRef);
        addVisit(BINARY_OP, this::visitBinExpr);
        addVisit(INTEGER_LITERAL, this::visitInteger);
        addVisit(BOOLEAN_LITERAL, this::visitBoolean);
        addVisit(FUNCTION_CALL, this::visitFunctionCall);
        addVisit(NEW_CLASS, this::visitNewClass);
        addVisit(NEGATION, this::visitNegation);
        addVisit(NEW_ARRAY, this::visitNewArray);
        addVisit(ARRAY_ACCESS, this::visitArrayAccess);
        addVisit(ARRAY_INIT, this::visitArrayInit);
        addVisit(LENGTH, this::visitLength);

        setDefaultVisit(this::defaultVisit);
    }


    private OllirExprResult visitInteger(JmmNode node, Void unused) {
        var intType = new Type(TypeUtils.getIntTypeName(), false);
        String ollirIntType = OptUtils.toOllirType(intType);
        String code = node.get("value") + ollirIntType;
        return new OllirExprResult(code);
    }

    private OllirExprResult visitBoolean(JmmNode node, Void unused) {
        var boolType = new Type("boolean", false);
        String ollirBoolType = OptUtils.toOllirType(boolType);
        String code;
        if(node.get("value").equals("true")) {
            code = "1" + ollirBoolType;
        }
        else if(node.get("value").equals("false")) {
            code = "0" + ollirBoolType;
        } else {
            code = node.get("value") + ollirBoolType;
        }

        return new OllirExprResult(code);
    }


    private OllirExprResult visitBinExpr(JmmNode node, Void unused) {
        System.out.println(" node:"+node);
        var lhs = visit(node.getJmmChild(0));
        var rhs = visit(node.getJmmChild(1));

        StringBuilder computation = new StringBuilder();
        //System.out.println(" lhs:"+lhs);
        //System.out.println(" rhs:"+rhs);

//        if(lhs.getComputation().equals("invoke") && rhs.getComputation().equals("invoke")) {
//
//        }

        // code to compute the children
        computation.append(lhs.getComputation());
        computation.append(rhs.getComputation());

        // code to compute self
        Type resType = TypeUtils.getExprType(node, table);
        String resOllirType = OptUtils.toOllirType(resType);
        String code = OptUtils.getTemp() + resOllirType;

        computation.append(code).append(SPACE)
                .append(ASSIGN).append(resOllirType).append(SPACE)
                .append(lhs.getCode()).append(SPACE);

        Type type = TypeUtils.getExprType(node, table);
        computation.append(node.get("op")).append(OptUtils.toOllirType(type)).append(SPACE)
                .append(rhs.getCode()).append(END_STMT);

        System.out.print(" Code:"+code+"\n");
        System.out.print(" Computation:"+computation+"\n");
        return new OllirExprResult(code, computation);
    }


    private OllirExprResult visitVarRef(JmmNode node, Void unused) {

        var id = node.get("name");
        //System.out.print(" VisitVarRef1:" + node  + "\n");
        //if(node.get("name").equals(node.get()))


        Type type = TypeUtils.getExprType(node, table);
        //System.out.print(" VisitVarRef5:" + type  + "\n");
        String ollirType = OptUtils.toOllirType(type);

        String code = id + ollirType;
        String computation = id + ollirType;
        //System.out.println(" VarRef:"+code);
        return new OllirExprResult(code);
    }

    private OllirExprResult visitFunctionCall(JmmNode node, Void unused) {
        //System.out.println(" ENTRAS AQUI?");
//        System.out.println(" node:"+node);
//        System.out.println(" a:"+node.getChildren());
//        System.out.println(" metodos:"+table.getMethods());
//        System.out.println(" Parent:"+node.getParent());
//        System.out.println(" isto:"+table.getMethods().get(2));
        String methodName = node.getAncestor(METHOD_DECL).map(method -> method.get("name")).orElseThrow();
        StringBuilder computation = new StringBuilder();
        Type retType;
        String temp = "";
        OllirExprResult childfunc = new OllirExprResult("", "");

        if(table.getReturnType(node.get("name")) != null) {
            retType = table.getReturnType(node.get("name"));
        } else {
            retType = new Type("void", false);
        }

        if(node.getChildren().size()>1) {
            for(int i=1; i<node.getChildren().size(); i++) {
                if(node.getChild(i).getKind().equals("FunctionCall") || node.getChild(i).getKind().equals("Length")) {
                    childfunc = visit(node.getChild(i));
                }
            }
        }
        computation.append(childfunc.getComputation());

//        System.out.println(" b:"+table.getReturnType(node.get("name")));

        String code = "";
        if(node.getParent().getKind().equals("BinaryOp") || node.getParent().getKind().equals("AssignStmt") || node.getParent().getKind().equals("FunctionCall")) {
            if (retType != null) {
                temp = OptUtils.getTemp() + OptUtils.toOllirType(retType);
            } else {
                temp = OptUtils.getTemp() + ".V";
            }

            computation.append(temp).append(SPACE);

            if(table.getMethods().contains(node.get("name"))) {
                code = temp;
            } else {
                code = temp;
            }

            computation.append(ASSIGN).append(OptUtils.toOllirType(retType)).append(SPACE);
        }

        if(table.getMethods().contains(node.get("name"))) {
            computation.append("invokevirtual");
        } else {
            computation.append("invokestatic");
        }

        computation.append("(");

        List<String> imports = table.getImports();
        List<String> modifiedImports = new ArrayList<>();

        for (String importString : imports) {
            String[] elements = importString.substring(1, importString.length() - 1).split(",");
            for (String element : elements) {
                modifiedImports.add(element.trim());
            }
        }

//        System.out.println(" BBBBBBBBBBBBBB:"+table.getImports().size());
//        System.out.println(" CCCCCCCCCCCCCC:"+node.getChild(0));
        if(node.getChild(0).hasAttribute("name")) {
            //System.out.println(" BBBBBBBBBBBBBB:"+table.getImports().get(0).contains(node.getChild(0).get("name")));
//            for(int i = 0;i<table.getImports().size();i++) {
//                if (table.getImports().get(i).contains(node.getChild(0).get("name"))) {
//                    computation.append(node.getChild(0).get("name"));
//                }
//                else {
//                    computation.append((node.getChild(0).get("name"))).append(".").append(table.getClassName());
//                }
//            }
            if(modifiedImports.contains(node.getChild(0).get("name"))) {
                computation.append(node.getChild(0).get("name"));
            }
            else {
                computation.append((node.getChild(0).get("name"))).append(".").append(table.getClassName());
            }
            //computation.append(node.getAncestor(VAR_DECL));
        } else {
            computation.append("this.").append(table.getClassName());
        }

        computation.append(", ");

        computation.append("\""+node.get("name")+"\"");
        //System.out.println(" AAAAAAAAAAAAAA:"+node.getChildren());

        if(node.getChildren().size()>1) {
            for(int i=1; i<node.getChildren().size(); i++) {
                computation.append(", ");
                if(node.getChild(i).getKind().equals("FunctionCall")) {
                    computation.append(childfunc.getCode());
                }
                else {
                    computation.append(visit(node.getChild(i)).getCode());
                }

            }
        }

        computation.append(")");



//        Type retType = table.getReturnType(node.get("name"));
        //System.out.println(" type:"+retType);
        if(table.getMethods().contains(node.get("name"))) {
            computation.append(OptUtils.toOllirType(retType));
        } else {
            computation.append(".V");
        }

        computation.append(END_STMT);
//        System.out.println(" Code Functional call:"+code);
//        System.out.println(" Code Functional callRet:"+codeRet);
        //System.out.print(code);
//        System.out.println(" FuncCall Computation:"+computation);
        return new OllirExprResult(code, computation.toString());
    }

    private OllirExprResult visitNewClass(JmmNode node, Void unused) {
//        System.out.println(" ENTRAS AQUI?:"+node);
//        System.out.println(" A:"+node.getParent());
        String methodName = node.getAncestor(METHOD_DECL).map(method -> method.get("name")).orElseThrow();
//        System.out.println(" variaveis:"+table.getLocalVariables(methodName).get(3));
        StringBuilder computation = new StringBuilder();
        String retType = "." + node.get("name");
        String temp = "";
        String code = "";

        temp = OptUtils.getTemp()  + retType;
//        System.out.println(" temp:"+temp);
        computation.append(temp).append(SPACE);

        if(table.getMethods().contains(node.get("name"))) {
            code = temp;
        } else {
            code = temp;
        }


        computation.append(ASSIGN).append(retType).append(SPACE);

        computation.append("new(").append(node.get("name")).append(")").append(retType).append(END_STMT);

        computation.append("invokespecial");

        computation.append("(");

        computation.append(temp);

        computation.append(", ");

        computation.append("\"<init>\"");

        computation.append(").V");

        computation.append(END_STMT);

//        for(int i = 0;i<table.getLocalVariables(methodName).size();i++) {
//            if(table.getLocalVariables(methodName).get(i).getName().equals(node.getParent().get("var"))) {
//                computation.append(visit(table.getLocalVariables(methodName).get(i).));
//            }
//        }

        //computation.append(node.getParent().get("var")).append(retType).append(SPACE).append(ASSIGN).append(retType).append(SPACE).append(temp);

        //System.out.println(computation);
        return new OllirExprResult(code, computation.toString());
    }

    private OllirExprResult visitNegation(JmmNode node, Void unused) {
        //System.out.println(" negation:"+node);
        Type resType = TypeUtils.getExprType(node, table);
        String resOllirType = OptUtils.toOllirType(resType);
        String code = node.get("value") + resOllirType + SPACE + visit(node.getChild(0)).getCode();
//        String code = "!" + resOllirType + SPACE + visit(node.getChild(0)).getCode();
        return new OllirExprResult(code);
    }

    private OllirExprResult visitNewArray(JmmNode node, Void unused) {
//        System.out.println(" NewArray:"+node);
        Type resType = TypeUtils.getExprType(node, table);
        String resOllirType = OptUtils.toOllirType(resType);
        String code = "new(array, " + visit(node.getChild(0)).getCode() + ")" + resOllirType ;
        return new OllirExprResult(code);
    }

    private OllirExprResult visitArrayAccess(JmmNode node, Void unused) {
//        System.out.println(" NewArray:"+node);
        StringBuilder computation = new StringBuilder();
        String temp = "";
        String code = "";
        Type resType = TypeUtils.getExprType(node.getChild(0), table);
        String resOllirType = OptUtils.toOllirType(resType);
        Type retArrayAcces = TypeUtils.getExprType(node, table);
        String retArrayAccesOllirType = OptUtils.toOllirType(retArrayAcces);
        temp = OptUtils.getTemp() + resOllirType;
//        System.out.println(" temp:"+temp);
        computation.append(temp).append(SPACE);

        code = temp;

        computation.append(ASSIGN).append(resOllirType).append(SPACE);

        if(node.getParent().getKind().equals("ReturnStmt")) {
            computation.append(node.getChild(0).get("name")).append(retArrayAccesOllirType).append("[").append(visit(node.getChild(1)).getCode()).append("]").append(resOllirType).append(END_STMT);
        }
        else {
            computation.append(node.getChild(0).get("name")).append("[").append(visit(node.getChild(1)).getCode()).append("]").append(resOllirType).append(END_STMT);
        }

        System.out.println(" Array Access:"+code);
        System.out.println(" Array Access:"+computation);
        return new OllirExprResult(code,computation);
    }

    private OllirExprResult visitArrayInit(JmmNode node, Void unused) {
        System.out.println(" NewArray:"+node);
        String methodName = node.getAncestor(METHOD_DECL).map(method -> method.get("name")).orElseThrow();
        StringBuilder computation = new StringBuilder();
        String temp = "";
        String code = "";
        Type retType = TypeUtils.getExprType(node.getChild(0), table);
        String arrayType = OptUtils.toOllirType(retType);

        for(int i = 0; i<table.getParameters(methodName).size(); i++) {
            if(table.getParameters(methodName).get(i).getName().equals(node.getParent().get("var"))) {
                //System.out.println(" parametro:"+table.getParameters(methodName).get(i).getType().getName());
                retType = table.getParameters(methodName).get(i).getType();
            }
        }

        for(int i = 0; i<table.getLocalVariables(methodName).size(); i++) {
            if(table.getLocalVariables(methodName).get(i).getName().equals(node.getParent().get("var"))) {
                //System.out.println(" parametro:"+table.getParameters(methodName).get(i).getType().getName());
                retType = table.getLocalVariables(methodName).get(i).getType();
            }
        }

        String resOllirType = OptUtils.toOllirType(retType);

        temp = OptUtils.getTemp() + resOllirType;
        code = "__varargs_array_0" + resOllirType;
        computation.append(temp).append(SPACE).append(ASSIGN).append(resOllirType).append(SPACE).append("new(array, ").append(node.getNumChildren()).append(arrayType).append(")").append(resOllirType).append(END_STMT);

        computation.append(code).append(SPACE).append(ASSIGN).append(resOllirType).append(SPACE).append(temp).append(END_STMT);

        for(int i = 0; i<node.getNumChildren(); i++) {
            computation.append(code).append("[").append(visit(node.getChild(i)).getCode()).append("]").append(arrayType).append(SPACE).append(ASSIGN).append(arrayType).append(SPACE).append(visit(node.getChild(i)).getCode()).append(END_STMT);
        }

        return new OllirExprResult(code, computation.toString());
    }

    private OllirExprResult visitLength(JmmNode node, Void unused) {
        StringBuilder computation = new StringBuilder();
        String temp = "";
        String code = "";
        Type lengthType = TypeUtils.getExprType(node, table);
        String resOllirType = OptUtils.toOllirType(lengthType);
        Type retType = TypeUtils.getExprType(node.getChild(0), table);
        String arrayType = OptUtils.toOllirType(retType);

        temp = OptUtils.getTemp() + resOllirType;
        code = temp;

        computation.append(temp).append(SPACE).append(ASSIGN).append(resOllirType).append(SPACE).append("arraylength(").append(visit(node.getChild(0)).getCode()).append(")").append(resOllirType).append(END_STMT);

        return new OllirExprResult(code, computation);
    }

    /**
     * Default visitor. Visits every child node and return an empty result.
     *
     * @param node
     * @param unused
     * @return
     */
    private OllirExprResult defaultVisit(JmmNode node, Void unused) {

        for (var child : node.getChildren()) {
            visit(child);
        }

        return OllirExprResult.EMPTY;
    }

}
