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
//        addVisit(FUNCTION_CALL, this::visitFunctionCall);

        setDefaultVisit(this::defaultVisit);
    }


    private String visitAssignStmt(JmmNode node, Void unused) {
//        System.out.println(" nodeAssign:"+node);
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
        Type thisType = TypeUtils.getExprType(node.getJmmChild(0), table);
        String typeString = OptUtils.toOllirType(thisType);


        //code.append(lhs.getCode());
        for(int i = 0; i<table.getLocalVariables(methodName).size(); i++) {
            if(table.getLocalVariables(methodName).get(i).getName().equals(node.get("var"))) {
                code.append(table.getLocalVariables(methodName).get(i).getName());
                code.append(OptUtils.toOllirType(table.getLocalVariables(methodName).get(i).getType()));
            }
        }

        code.append(SPACE);

        code.append(ASSIGN);
        code.append(typeString);
        code.append(SPACE);

        code.append(rhs.getCode());

        code.append(END_STMT);

//        System.out.println(" code assign:"+code);
        return code.toString();
    }


    private String visitReturn(JmmNode node, Void unused) {

        String methodName = node.getAncestor(METHOD_DECL).map(method -> method.get("name")).orElseThrow();
        Type retType = table.getReturnType(methodName);
        //System.out.print(" Return qual é?: "+node.getChild(0)+"\n");
        //System.out.print(" Return type: " + retType + "\n");
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

        StringBuilder code = new StringBuilder(".method ");

        boolean isPublic = NodeUtils.getBooleanAttribute(node, "isPublic", "false");

        if (isPublic) {
            code.append("public ");
        }

        boolean isStatic = NodeUtils.getBooleanAttribute(node, "isStatic", "false");

        if(isStatic) {
            code.append("static ");
        }

        // name
        var name = node.get("name");
        code.append(name);

        // param
        if(PARAM.check(node.getChild(1))) {
//            var Param = 0;
//            for (int i = Param; i < node.getNumChildren(); i++) {
//                var paramCode = visit(node.getJmmChild(i));
//
//                code.append("(" + paramCode + ")");
//            }
            code.append("(");
            var paramCode = table.getParameters(name);
            for(Symbol param : paramCode) {
                //System.out.print(" Node Param:"+param+"\n");
                code.append(param.getName());
                if(param.getType().isArray())
                    code.append(".array");
                var paramType = OptUtils.toOllirType(param.getType());
                code.append(paramType);
                if(!(paramCode.indexOf(param) == (paramCode.size() -1))){
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
//                System.out.println(" type:"+child.getChild(0));
//                System.out.println(" que?:"+table.getLocalVariables(name));
                if(!(VAR_DECL.check(child)) && !(PARAM.check(child))) {
//                    if(child.getChild(0).getKind().equals("Id")) {
//
//                    }
                    var childCode = visit(child);
//                    System.out.println(" ChildCode:"+childCode);
                    code.append(childCode);
                }
                if(FUNCTION_CALL.check(funcCall)) {
                    var childCode = exprVisitor.visit(funcCall);
                    //System.out.print(" function call:"+childCode.getCode()+"\n");
                    code.append(childCode.getComputation());
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
                if(FUNCTION_CALL.check(funcCall)) {
                    var childCode = exprVisitor.visit(funcCall);
                    //System.out.print(" function call:"+childCode+"\n");
                    code.append(childCode.getComputation());
                }
            }
        }



        code.append(R_BRACKET);
        code.append(NL);

        System.out.print(code);
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
            System.out.print(child);

            var result = visit(child);

            if (METHOD_DECL.check(child) && needNl) {
                code.append(NL);
                needNl = false;
            }

            code.append(result);
        }

        code.append(buildConstructor());
        code.append(R_BRACKET);

        System.out.print(code);
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

        System.out.println(code);
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
        System.out.print(code);
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
        //System.out.println(" ENTRAS AQUI?:"+node);
        var voidType = new Type("void", false);
        StringBuilder code = new StringBuilder();
        code.append(OptUtils.toOllirType(voidType));

        //System.out.println(code);
        return code.toString();
    }



//    private String visitFunctionCall(JmmNode node, Void unused) {
//        System.out.print(" SERA?:"+node+"\n");
//        StringBuilder code = new StringBuilder("invokestatic");
//
//        code.append("(");
//
//        code.append((node.getChild(0).get("name")));
//
//        code.append(", ");
//
//        code.append("\""+node.get("name")+"\", ");
//        System.out.print(" Child disto:"+node.getChild(1)+"\n");
//        code.append(exprVisitor.visit(node.getChild(1)).getCode());
//
//        code.append(")");
//
//        code.append(".V");
//
//        code.append(END_STMT);
//        System.out.print(code);
//        return code.toString();
//    }


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
