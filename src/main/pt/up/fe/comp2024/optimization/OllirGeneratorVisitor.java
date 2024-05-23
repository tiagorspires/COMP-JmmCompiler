package pt.up.fe.comp2024.optimization;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp2024.ast.NodeUtils;
import pt.up.fe.comp2024.ast.TypeUtils;

import static pt.up.fe.comp2024.ast.Kind.*;

/**
 * Generates OLLIR code from JmmNodes that are not expressions.
 */
public class OllirGeneratorVisitor extends AJmmVisitor<Void, String> {

    private static final String SPACE = " ";
    private static final String ASSIGN = ":=";
    private final String END_STMT = ";\n";
    private final String NL = "\n";
    private final String L_BRACKET = " {\n";
    private final String R_BRACKET = "}\n";


    private final SymbolTable table;

    private final OllirExprGeneratorVisitor exprVisitor;

    public OllirGeneratorVisitor(SymbolTable table) {
        this.table = table;
        exprVisitor = new OllirExprGeneratorVisitor(table);
        this.buildVisitor();
    }


    @Override
    protected void buildVisitor() {

        addVisit(PROGRAM, this::visitProgram);
        addVisit(CLASS_DECL, this::visitClass);
        addVisit(METHOD_DECL, this::visitMethodDecl);
        addVisit(PARAM, this::visitParam);
        addVisit(RETURN_STMT, this::visitReturn);
        addVisit(ASSIGN_STMT, this::visitAssignStmt);
        addVisit(VAR_DECL, this::visitVar);
        addVisit(INTEGER, this::visitInteger);
        addVisit(BOOLEAN, this::visitBoolean);
        addVisit("String", this::visitString);
        addVisit("ImportDecl", this::visitImport);
        addVisit("Void", this::visitVoid);
        addVisit("Id", this::visitId);
        addVisit("ArrayAssign", this::visitArrayAssign);
        addVisit("Array", this::visitArray);
        addVisit("IfElseStmt", this::visitIfElse);
        addVisit("WhileStmt", this::visitWhile);
        addVisit("StmtScope", this::visitStmt);

        setDefaultVisit(this::defaultVisit);
    }


    private String visitAssignStmt(JmmNode node, Void unused) {
        System.out.println(" nodeAssign:"+node);
//        System.out.println(" Num Childs:"+node.getChild(0));
        StringBuilder code = new StringBuilder();
        String methodName = node.getAncestor(METHOD_DECL).map(method -> method.get("name")).orElseThrow();
//        System.out.print(" Assign:"+node.get("var")+"\n");
//        System.out.print(" Local Variables:"+table.getLocalVariables(methodName)+"\n");
//        for(int i = 0; i<table.getLocalVariables(methodName).size(); i++) {
//            if(table.getLocalVariables(methodName).get(i).getName().equals(node.get("var"))) {
//                code.append(table.getLocalVariables(methodName).get(i).getName());
//                code.append(OptUtils.toOllirType(table.getLocalVariables(methodName).get(i).getType()));
//            }
//        }
//        System.out.print(" code ta certo?:"+code+"\n");
        //var lhs = exprVisitor.visit(node.getJmmChild(0));
        var rhs = exprVisitor.visit(node.getJmmChild(0));

        //StringBuilder code = new StringBuilder();

        // code to compute the children
        //code.append(lhs.getComputation());
        code.append(rhs.getComputation());

        // code to compute self
        // statement has type of lhs
//        System.out.println(node);


        Type thisType = TypeUtils.getExprType(node.getJmmChild(0), table);
        String typeString = OptUtils.toOllirType(thisType);

        //code.append(lhs.getCode());
        //if(node.get("var").equals)
        boolean Local = false;
        for(int i = 0; i<table.getLocalVariables(methodName).size(); i++) {
            if(table.getLocalVariables(methodName).get(i).getName().equals(node.get("var"))) {
                code.append(table.getLocalVariables(methodName).get(i).getName());
                if(node.getChild(0).getKind().equals("NewClass")){
                    code.append(OptUtils.toOllirType(new Type(node.getChild(0).get("name"), false)));
                } else {
                    code.append(OptUtils.toOllirType(table.getLocalVariables(methodName).get(i).getType()));
                }
                Local = true;
            }
        }

        boolean Param = false;
        if(!Local) {
            for (int i = 0; i < table.getParameters(methodName).size(); i++) {
                if (table.getParameters(methodName).get(i).getName().equals(node.get("var"))) {
                    code.append(table.getParameters(methodName).get(i).getName());
                    if (node.getChild(0).getKind().equals("NewClass")) {
                        code.append(OptUtils.toOllirType(new Type(node.getChild(0).get("name"), false)));
                    } else {
                        code.append(OptUtils.toOllirType(table.getParameters(methodName).get(i).getType()));
                    }
                    Param = true;
                }
            }
        }

        if(!Local && !Param) {
            for (int i = 0; i < table.getFields().size(); i++) {
                if (table.getFields().get(i).getName().equals(node.get("var"))) {
//                    code.append(table.getFields().get(i).getName());
                    code.append("putfield(this.").append(table.getClassName()).append(", ").append(table.getFields().get(i).getName());
                    if (node.getChild(0).getKind().equals("NewClass")) {
                        code.append(OptUtils.toOllirType(new Type(node.getChild(0).get("name"), false)));
                    } else {
                        code.append(OptUtils.toOllirType(table.getFields().get(i).getType()));
                    }
                    code.append(", ").append(rhs.getCode()).append(")").append(".V");
                }
            }
        }

        if(Local == true || Param == true) {
            code.append(SPACE);

            code.append(ASSIGN);
            code.append(typeString);
            code.append(SPACE);

            code.append(rhs.getCode());
        }

        code.append(END_STMT);

        System.out.println(" code assign:"+code);
        return code.toString();
    }


    private String visitReturn(JmmNode node, Void unused) {
        String methodName = node.getAncestor(METHOD_DECL).map(method -> method.get("name")).orElseThrow();
        Type retType = table.getReturnType(methodName);
        //System.out.print(" Return qual é?: "+node.getChild(0)+"\n");
        //System.out.print(" Return type: " + retType + "\n");
        //System.out.println(node);
        StringBuilder code = new StringBuilder();

        var expr = OllirExprResult.EMPTY;

        if (node.getNumChildren() > 0) {
            //System.out.print(" Está certo?: sim "+node.getJmmChild(0)+"\n");
            expr = exprVisitor.visit(node.getJmmChild(0));
        }
        //System.out.print(" Antes ou depois?" + expr + "\n");
        code.append(expr.getComputation());
        code.append("ret");
        code.append(OptUtils.toOllirType(retType));
        code.append(SPACE);
        //System.out.print(code);

        code.append(expr.getCode());
        //System.out.print(code);
        code.append(END_STMT);

        //System.out.print(code);
        return code.toString();
    }


    private String visitParam(JmmNode node, Void unused) { // NAO ENTRA AQUI PORQUE FAÇO COM A SYMBOLTABLE NO METHODDECL

        //System.out.print(" Node Param:"+node+"\n");
        var typeCode = visit(node.getJmmChild(0));
        var id = node.get("name");

        String code = id + typeCode;

        //System.out.print(code);
        return code;
    }


    private String visitMethodDecl(JmmNode node, Void unused) {
//        System.out.println(" Aqui:"+node.getChildren());
//        System.out.println(" Aqui2:"+table.getMethods());
        boolean retExists = false;
        StringBuilder code = new StringBuilder(".method ");
        boolean hasEllipsis = NodeUtils.getBooleanAttribute(node, "hasEllipsis", "false");

        boolean isPublic = NodeUtils.getBooleanAttribute(node, "isPublic", "false");

        if (isPublic) {
            code.append("public ");
        }

        boolean isStatic = NodeUtils.getBooleanAttribute(node, "isStatic", "false");

        if (isStatic) {
            code.append("static ");
        }

        // name
        var name = node.get("name");
        code.append(name);

        // param
        if (PARAM.check(node.getChild(1)) || hasEllipsis) {
//            var Param = 0;
//            for (int i = Param; i < node.getNumChildren(); i++) {
//                var paramCode = visit(node.getJmmChild(i));
//
//                code.append("(" + paramCode + ")");
//            }
            code.append("(");
            var paramCode = table.getParameters(name);
            for (Symbol param : paramCode) {
                //System.out.print(" Node Param:"+param+"\n");
                code.append(param.getName());
//                if(param.getType().isArray())
//                    code.append(".array");
                var paramType = OptUtils.toOllirType(param.getType());
                code.append(paramType);
                if (!(paramCode.indexOf(param) == (paramCode.size() - 1))) {
                    code.append(", ");
                }
            }
            code.append(")");
//            var paramCode = visit(node.getJmmChild(1));
            //System.out.print(code);
            //code.append("(" + paramCode + ")");
            //System.out.print(code);
        } else {
            code.append("()");
        }

        // type
        //var retType = OptUtils.toOllirType(node.getJmmChild(0));
        //System.out.println(" ???:"+node.getJmmChild(0));
        var retType = visit(node.getJmmChild(0));
        //System.out.print(" RetType:"+retType+"\n");
        code.append(retType);
        code.append(L_BRACKET);

        // rest of its children stmts
        if(PARAM.check(node.getChild(1))) {
            var afterParam = 1;
            for (int i = afterParam; i < node.getNumChildren(); i++) {
                var child = node.getJmmChild(i);
                var funcCall = child.getChild(0);
//                System.out.println(" Child:"+child);
//                System.out.println(" ChildDoChild:"+child.getChild(0));
                //                System.out.println(" type:"+child.getChild(0));
//                System.out.println(" que?:"+table.getLocalVariables(name));
//                if(VAR_DECL.check(child) && funcCall.getKind().equals("Id")) {
//                    visit(child);
//                }
                if(!(VAR_DECL.check(child)) && !(PARAM.check(child))) {
//                    if(child.getChild(0).getKind().equals("Id")) {
//
//                    }
                    var childCode = visit(child);
//                    System.out.println(" ChildCode:"+childCode);
                    code.append(childCode);
                }
                if(FUNCTION_CALL.check(funcCall) && child.getKind().equals("ExprStmt")) {
                    var childCode = exprVisitor.visit(funcCall);
                    //System.out.print(" function call:"+childCode.getCode()+"\n");
                    System.out.println(" Computation:"+childCode.getComputation());
                    code.append(childCode.getComputation());
                }
                if(RETURN_STMT.check(child)) {
                    retExists = true;
                }
            }
        } else {
            var afterParam = 1;
            for (int i = afterParam; i < node.getNumChildren(); i++) {
                var child = node.getJmmChild(i);
                var funcCall = child.getChild(0);
//                System.out.print(" Child:"+child+"\n");
//                System.out.print(" Child do Child:"+funcCall+"\n");
                if(!(VAR_DECL.check(child))) {
                    var childCode = visit(child);
                    code.append(childCode);
                }
                if(FUNCTION_CALL.check(funcCall) && child.getKind().equals("ExprStmt")) {
                    var childCode = exprVisitor.visit(funcCall);
                    //System.out.print(" function call:"+childCode+"\n");
                    System.out.println(childCode.getComputation());
                    code.append(childCode.getComputation());
                }
                if(RETURN_STMT.check(child)) {
                    retExists = true;
                }
            }
        }

        if(!retExists) {
            code.append("ret.V").append(END_STMT);
        }

        code.append(R_BRACKET);
        code.append(NL);

        //System.out.print(code);
        return code.toString();
    }


    private String visitClass(JmmNode node, Void unused) {

        StringBuilder code = new StringBuilder();
        String teste = "null";
        code.append(table.getClassName());
        //System.out.print(table.getClass().toString());
        //System.out.println(table.getSuper().equals(null));
        //System.out.print("SUPER?: "+node.getObject("extendClassName")+"\n");
//
        if(node.hasAttribute("extendClassName")) {
           code.append(" extends ");
           code.append(table.getSuper());
        }

        code.append(L_BRACKET);

        code.append(NL);
        var needNl = true;

        for (var child : node.getChildren()) {
            //System.out.print(child);

            var result = visit(child);

            if (METHOD_DECL.check(child) && needNl) {
                code.append(NL);
                needNl = false;
            }

            code.append(result);
        }

        code.append(buildConstructor());
        code.append(R_BRACKET);

        //System.out.print(code);
        return code.toString();
    }

    private String buildConstructor() {

        return ".construct " + table.getClassName() + "().V {\n" +
                "invokespecial(this, \"<init>\").V;\n" +
                "}\n";
    }


    private String visitProgram(JmmNode node, Void unused) {

        StringBuilder code = new StringBuilder();

        node.getChildren().stream()
                .map(this::visit)
                .forEach(code::append);

        return code.toString();
    }

    private String visitVar(JmmNode node, Void unused) {
        StringBuilder code = new StringBuilder();

        boolean isPublic = NodeUtils.getBooleanAttribute(node, "isPublic", "false");

        if(CLASS_DECL.check(node.getParent())) {
            code = new StringBuilder(".field ");

            if (isPublic) {
                code.append("public ");
            } else {
                code.append("private ");
            }
        }

        if(node.getChild(0).getKind().equals("[]")) {
            code.append(node.get("name"));
            code.append(".array");
            code.append(visit(node.getChild(0)));
        } else {
            code.append(node.get("name"));
            //if(node.getChild(0).equals("Id"))
            code.append(visit(node.getChild(0)));
        }

        code.append(END_STMT);

        //System.out.println(code);
        return code.toString();
    }

    private String visitInteger(JmmNode node, Void unused) {
        var intType = new Type("int", false);
        StringBuilder code = new StringBuilder();
        code.append(OptUtils.toOllirType(intType));

        //System.out.println(code);
        return code.toString();
    }

    private String visitBoolean(JmmNode node, Void unused) {
        var boolType = new Type("boolean", false);
        StringBuilder code = new StringBuilder();
        code.append(OptUtils.toOllirType(boolType));

        //System.out.println(code);
        return code.toString();
    }

    private String visitString(JmmNode node, Void unused) {
        var arrayType = new Type("String", true);
        StringBuilder code = new StringBuilder();
        code.append(OptUtils.toOllirType(arrayType));

        return code.toString();
    }

    private String visitImport(JmmNode node, Void unused) {
        StringBuilder code = new StringBuilder();

        code.append("import ");
        code.append(node.get("value"));
        code.append(END_STMT);
        for (int i = 0; i < code.length(); i++) {
            if (code.charAt(i) == '[' || code.charAt(i) == ']') {
                code.deleteCharAt(i);
                i--;
            }
        }
        //System.out.print(code);
        return code.toString();
    }

    private String visitVoid(JmmNode node, Void unused) {
        var voidType = new Type("void", false);
        StringBuilder code = new StringBuilder();
        code.append(OptUtils.toOllirType(voidType));

        //System.out.println(code);
        return code.toString();
    }

    private String visitId(JmmNode node, Void unused) {
        var IdType = new Type(node.get("name"), false);
        StringBuilder code = new StringBuilder();
        code.append(OptUtils.toOllirType(IdType));

        //System.out.println(code);
        return code.toString();
    }

    private String visitArrayAssign(JmmNode node, Void unused) {
//        System.out.println(" Array?:"+node);
//        StringBuilder computation = new StringBuilder();
//        String code = "";
//        String methodName = node.getAncestor(METHOD_DECL).map(method -> method.get("name")).orElseThrow();
//        Type resType = TypeUtils.getExprType(node.getChild(0), table);
//        String resOllirType = OptUtils.toOllirType(resType);
//
//        for(int i = 0; i<table.getParameters(methodName).size(); i++) {
//            if(table.getParameters(methodName).get(i).getName().equals(node.get("var"))) {
//                //System.out.println(" parametro:"+table.getParameters(methodName).get(i).getType().getName());
//                computation.append(table.getParameters(methodName).get(i).getType());
//            }
//        }
//
//        computation.append(node.get("var")).append("[").append(visit(node.getChild(0))).append("]").append(resOllirType).append(SPACE);
//
//        computation.append(ASSIGN).append(resOllirType).append(SPACE);
//
//        computation.append(visit(node.getChild(1))).append(END_STMT);
//        System.out.println(" Array Access:"+code);
//        System.out.println(" Array Access:"+computation);
//        return computation.toString();






        StringBuilder code = new StringBuilder();
        String methodName = node.getAncestor(METHOD_DECL).map(method -> method.get("name")).orElseThrow();

        var position = exprVisitor.visit(node.getJmmChild(0));
        var value = exprVisitor.visit(node.getJmmChild(1));


        Type thisType = TypeUtils.getExprType(node.getJmmChild(1), table);
        String typeString = OptUtils.toOllirType(thisType);

        for(int i = 0; i<table.getLocalVariables(methodName).size(); i++) {
            if(table.getLocalVariables(methodName).get(i).getName().equals(node.get("var"))) {
                //code.append(table.getLocalVariables(methodName).get(i).getName()).append("[").append(visit(node.getChild(0))).append("]").append(typeString);;
                if(node.getChild(0).getKind().equals("NewClass")){
                    code.append(OptUtils.toOllirType(new Type(node.getChild(0).get("name"), false)));
                } else {
                    code.append(node.get("var")).append("[").append(position.getCode()).append("]").append(typeString);

                }
            }
        }

        for(int i = 0; i<table.getParameters(methodName).size(); i++) {
            if(table.getParameters(methodName).get(i).getName().equals(node.get("var"))) {
                //code.append(table.getParameters(methodName).get(i).getName()).append("[").append(visit(node.getChild(0))).append("]").append(typeString);
                if(node.getChild(0).getKind().equals("NewClass")){
                    code.append(OptUtils.toOllirType(new Type(node.getChild(0).get("name"), false)));
                } else {
                    code.append(node.get("var")).append("[").append(position.getCode()).append("]").append(typeString);
                }
            }
        }

        code.append(SPACE);

        code.append(ASSIGN);
        code.append(typeString);
        code.append(SPACE);

        code.append(value.getCode());

        code.append(END_STMT);

        System.out.println(" code assign:"+code);
        return code.toString();
    }

    private String visitArray(JmmNode node, Void unused) {
        Type arrayType;
        if(node.getChild(0).getKind().equals("Integer")) {
            arrayType = new Type("int", true);
        } else if(node.getChild(0).getKind().equals("Boolean")) {
            arrayType = new Type("boolean", true);
        } else {
            arrayType = new Type(node.getKind(), true);
        }
        StringBuilder code = new StringBuilder();
        code.append(OptUtils.toOllirType(arrayType));

        System.out.println(code);
        return code.toString();
    }

    private String visitIfElse(JmmNode node, Void unused) {
        StringBuilder code = new StringBuilder();
        String ifbody = "ifbody_";
        String end = "endif_";
        int ifNum = OptUtils.getNextIfNum();

        if(node.getChild(0).getKind().equals("BinaryOp")) {
            var binOp = exprVisitor.visit(node.getChild(0));
            code.append(binOp.getComputation());

            code.append("if( ").append(binOp.getCode()).append(" ) ").append("goto ").append(ifbody).append(ifNum).append(END_STMT);

            code.append(visit(node.getChild(2)));
            code.append("goto ").append(end).append(ifNum).append(END_STMT);
            code.append(ifbody).append(ifNum).append(":\n");
            code.append(visit(node.getChild(1)));
            code.append(end).append(ifNum).append(":\n");
        } else {

            code.append("if( ").append(exprVisitor.visit(node.getChild(0)).getCode()).append(" ) ").append("goto ").append(ifbody).append(ifNum).append(END_STMT);

            code.append(visit(node.getChild(2)));
            code.append("goto ").append(end).append(ifNum).append(END_STMT);
            code.append(ifbody).append(ifNum).append(":\n");
            code.append(visit(node.getChild(1)));
            code.append(end).append(ifNum).append(":\n");
        }


        return code.toString();
    }

    private String visitWhile(JmmNode node, Void unused) {
        StringBuilder code = new StringBuilder();
        String whilebody = "whilebody_";
        String end = "endwhile_";
        int ifNum = OptUtils.getNextWhileNum();

        if(node.getChild(0).getKind().equals("BinaryOp")) {
            code.append("Loop:\n");
            var binOp = exprVisitor.visit(node.getChild(0));
            code.append(binOp.getComputation());

            code.append("if( ").append(binOp.getCode()).append(" ) ").append("goto ").append(whilebody).append(ifNum).append(END_STMT);

            code.append("goto ").append(end).append(ifNum).append(END_STMT);
            code.append(whilebody).append(ifNum).append(":\n");
            code.append(visit(node.getChild(1)));
            code.append("goto Loop").append(END_STMT);
            code.append(end).append(ifNum).append(":\n");
        } else {
            code.append("Loop:\n");
            code.append("if( ").append(exprVisitor.visit(node.getChild(0)).getCode()).append(" ) ").append("goto ").append(whilebody).append(ifNum).append(END_STMT);

            code.append("goto ").append(end).append(ifNum).append(END_STMT);
            code.append(whilebody).append(ifNum).append(":\n");
            code.append(visit(node.getChild(1)));
            code.append("goto Loop").append(END_STMT);
            code.append(end).append(ifNum).append(":\n");
        }


        return code.toString();
    }

    private String visitStmt(JmmNode node, Void unused) {
        StringBuilder code = new StringBuilder();

        if(node.getChild(0).getKind().equals("ifElseStmt") || node.getChild(0).getKind().equals("WhileStmt")) {
            code.append(visit(node.getChild(0)));
        } else {
            code.append(exprVisitor.visit(node.getChild(0)).getComputation());
            code.append(visit(node.getChild(0)));
        }

        return code.toString();
    }



    /**
     * Default visitor. Visits every child node and return an empty string.
     *
     * @param node
     * @param unused
     * @return
     */
    private String defaultVisit(JmmNode node, Void unused) {
        for (var child : node.getChildren()) {
            visit(child);
        }

        return "";
    }
}
