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
        addVisit("ExprStmt", this::visitExprStmt);

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


        Type type = TypeUtils.getExprType(node, table);


        if(node.get("op").equals("<") || node.get("op").equals(">")) {
            int ifNum = OptUtils.getNextIfNum();

            computation.append("if(").append(lhs.getCode()).append(SPACE);

            computation.append(node.get("op")).append(OptUtils.toOllirType(type)).append(SPACE)
                    .append(rhs.getCode()).append(") ").append("goto ").append("true_").append(ifNum).append(END_STMT);

            computation.append(code).append(SPACE)
                    .append(ASSIGN).append(resOllirType).append(SPACE)
                    .append("0").append(resOllirType).append(END_STMT);

            computation.append("goto ").append("end_").append(ifNum).append(END_STMT);

            computation.append("true_").append(ifNum).append(":\n");

            computation.append(code).append(SPACE)
                    .append(ASSIGN).append(resOllirType).append(SPACE)
                    .append("1").append(resOllirType).append(END_STMT);

            computation.append("end_").append(ifNum).append(":\n");
        } else if(node.get("op").equals("&&") || node.get("op").equals("||")) {

        } else {

            computation.append(code).append(SPACE)
                    .append(ASSIGN).append(resOllirType).append(SPACE)
                    .append(lhs.getCode()).append(SPACE);

            computation.append(node.get("op")).append(OptUtils.toOllirType(type)).append(SPACE)
                    .append(rhs.getCode()).append(END_STMT);
        }
        System.out.print(" Code:"+code+"\n");
        System.out.print(" Computation:"+computation+"\n");
        return new OllirExprResult(code, computation);
    }


    private OllirExprResult visitVarRef(JmmNode node, Void unused) {
        StringBuilder computation = new StringBuilder();
        String methodName = node.getAncestor(METHOD_DECL).map(method -> method.get("name")).orElseThrow();
        String code = "";
        String temp = "";
        var id = node.get("name");

        Type type = TypeUtils.getExprType(node, table);
        String ollirType = OptUtils.toOllirType(type);

        boolean Local = false;
        for(int i = 0; i<table.getLocalVariables(methodName).size(); i++) {
            if(table.getLocalVariables(methodName).get(i).getName().equals(node.get("name"))) {
                code = id + ollirType;
                Local = true;
            }
        }

        boolean Param = false;
        if(!Local) {
            for (int i = 0; i < table.getParameters(methodName).size(); i++) {
                if (table.getParameters(methodName).get(i).getName().equals(node.get("name"))) {
                    code = id + ollirType;
                    Param = true;
                }
            }
        }

        boolean Field = false;
        if(!Local && !Param) {
            for (int i = 0; i < table.getFields().size(); i++) {
                if (table.getFields().get(i).getName().equals(node.get("name"))) {
                    code = OptUtils.getTemp() + ollirType;
                    Field = true;
                }
            }
        }

        if(Field) {
            computation.append(code).append(SPACE).append(ASSIGN).append(ollirType).append(SPACE).append("getfield(this.").append(table.getClassName()).append(", ").append(id).append(ollirType).append(")").append(ollirType).append(END_STMT);
        }

        return new OllirExprResult(code, computation);
    }

    private OllirExprResult visitFunctionCall(JmmNode node, Void unused) {
        String methodName = node.getAncestor(METHOD_DECL).map(method -> method.get("name")).orElseThrow();
        StringBuilder computation = new StringBuilder();
        Type retType;
        String temp = "";
        OllirExprResult childfunc = new OllirExprResult("", "");
        boolean Local = false;
        boolean Param = false;
        boolean Field = false;


        if(table.getReturnType(node.get("name")) != null) {
            retType = table.getReturnType(node.get("name"));
        } else {
            retType = new Type("void", false);
        }


        if(node.getChildren().size()>1) {
            for(int i=1; i<node.getChildren().size(); i++) {
                childfunc = visit(node.getChild(i));
//                if(!(node.getChild(i).getKind().equals("IntegerLiteral") || node.getChild(i).getKind().equals("BooleanLiteral") || node.getChild(i).getKind().equals("ArrayInit"))) {
//                    for (int j = 0; j < table.getLocalVariables(methodName).size(); j++) {
//                        if (table.getLocalVariables(methodName).get(j).getName().equals(node.getChild(i).get("name"))) {
//                            Local = true;
//                        }
//                    }
//
//                    if (!Local) {
//                        for (int j = 0; j < table.getParameters(methodName).size(); j++) {
//                            if (table.getParameters(methodName).get(j).getName().equals(node.getChild(i).get("name"))) {
//                                Param = true;
//                            }
//                        }
//                    }
//
//                    if (!Local && !Param) {
//                        for (int j = 0; j < table.getFields().size(); j++) {
//                            if (table.getFields().get(j).getName().equals(node.getChild(i).get("name"))) {
//                                childfunc = visit(node.getChild(i));
//                                Field = true;
//                            }
//                        }
//                    }
//
//                    if (node.getChild(i).getKind().equals("FunctionCall") || node.getChild(i).getKind().equals("Length") || Field) {
//                        childfunc = visit(node.getChild(i));
//                    }
//                }

//                for (int j = 0; j < table.getLocalVariables(methodName).size(); j++) {
//                    if (table.getLocalVariables(methodName).get(j).getName().equals(node.getChild(i).get("name"))) {
//                        Local = true;
//                    }
//                }
//
//                if (!Local) {
//                    for (int j = 0; j < table.getParameters(methodName).size(); j++) {
//                        if (table.getParameters(methodName).get(j).getName().equals(node.getChild(i).get("name"))) {
//                            Param = true;
//                        }
//                    }
//                }
//
//                if (!Local && !Param) {
//                    for (int j = 0; j < table.getFields().size(); j++) {
//                        if (table.getFields().get(j).getName().equals(node.getChild(i).get("name"))) {
//                            childfunc = visit(node.getChild(i));
//                            Field = true;
//                        }
//                    }
//                }
//
//                if (node.getChild(i).getKind().equals("FunctionCall") || node.getChild(i).getKind().equals("Length") || Field) {
//                    childfunc = visit(node.getChild(i));
//                }
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
//                if(node.getChild(i).getKind().equals("FunctionCall") || table.getFields().contains(node.getChild(i)) || Field) {
//                    computation.append(childfunc.getCode());
//                }
//                else {
//                    computation.append(visit(node.getChild(i)).getCode());
//                }
                if(node.getChildren().size() > 2) {
                    computation.append(visit(node.getChild(i)).getCode());
                } else {
                    computation.append(childfunc.getCode());
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

        return new OllirExprResult(code, computation.toString());
    }

    private OllirExprResult visitNewClass(JmmNode node, Void unused) {
        String methodName = node.getAncestor(METHOD_DECL).map(method -> method.get("name")).orElseThrow();
        StringBuilder computation = new StringBuilder();
        String retType = "." + node.get("name");
        String temp = "";
        String code = "";

        temp = OptUtils.getTemp()  + retType;

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
        Type resType = TypeUtils.getExprType(node, table);
        String resOllirType = OptUtils.toOllirType(resType);
        String code = node.get("value") + resOllirType + SPACE + visit(node.getChild(0)).getCode();
//        String code = "!" + resOllirType + SPACE + visit(node.getChild(0)).getCode();
        return new OllirExprResult(code);
    }

    private OllirExprResult visitNewArray(JmmNode node, Void unused) {
        Type resType = TypeUtils.getExprType(node, table);
        String resOllirType = OptUtils.toOllirType(resType);
        String code = "new(array, " + visit(node.getChild(0)).getCode() + ")" + resOllirType ;
        return new OllirExprResult(code);
    }

    private OllirExprResult visitArrayAccess(JmmNode node, Void unused) {
        StringBuilder computation = new StringBuilder();
        String temp = "";
        String code = "";
        Type resType = TypeUtils.getExprType(node.getChild(0), table);
        String resOllirType = OptUtils.toOllirType(resType);
        Type retArrayAcces = TypeUtils.getExprType(node, table);
        String retArrayAccesOllirType = OptUtils.toOllirType(retArrayAcces);
        temp = OptUtils.getTemp() + retArrayAccesOllirType;

        computation.append(temp).append(SPACE);

        code = temp;

        computation.append(ASSIGN).append(retArrayAccesOllirType).append(SPACE);

        if(node.getParent().getKind().equals("ReturnStmt")) {
            computation.append(node.getChild(0).get("name")).append("[").append(visit(node.getChild(1)).getCode()).append("]").append(retArrayAccesOllirType).append(END_STMT);
        }
        else {
            computation.append(node.getChild(0).get("name")).append("[").append(visit(node.getChild(1)).getCode()).append("]").append(retArrayAccesOllirType).append(END_STMT);
        }

        System.out.println(" Array Access:"+code);
        System.out.println(" Array Access:"+computation);
        return new OllirExprResult(code,computation);
    }

    private OllirExprResult visitArrayInit(JmmNode node, Void unused) {
        String methodName = node.getAncestor(METHOD_DECL).map(method -> method.get("name")).orElseThrow();
        StringBuilder computation = new StringBuilder();
        String temp = "";
        String code = "";
        Type retType = TypeUtils.getExprType(node.getChild(0), table);
        String arrayType = OptUtils.toOllirType(retType);

        for(int i = 0; i<table.getParameters(methodName).size(); i++) {
            if(table.getParameters(methodName).get(i).getName().equals(node.getParent().get("var"))) {
                retType = table.getParameters(methodName).get(i).getType();
            }
        }

        for(int i = 0; i<table.getLocalVariables(methodName).size(); i++) {
            if(table.getLocalVariables(methodName).get(i).getName().equals(node.getParent().get("var"))) {
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

    private OllirExprResult visitExprStmt(JmmNode node, Void unused) {
        StringBuilder computation = new StringBuilder();
        String code = "";

        computation.append(visit(node.getChild(0)).getComputation());
        code = visit(node.getChild(0)).getCode();

        return new OllirExprResult(code, computation);
    }

    /**
     * Default visitor. Visits every child node and return an empty result.
     *
     * @param node
     * @param unused
     * @return
     */
    private OllirExprResult  defaultVisit(JmmNode node, Void unused) {

        for (var child : node.getChildren()) {
            visit(child);
        }

        return OllirExprResult.EMPTY;
    }

}
