package pt.up.fe.comp2024.symboltable;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp2024.ast.Kind;
import pt.up.fe.comp2024.ast.TypeUtils;
import pt.up.fe.specs.util.SpecsCheck;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static pt.up.fe.comp2024.ast.Kind.METHOD_DECL;
import static pt.up.fe.comp2024.ast.Kind.VAR_DECL;

public class JmmSymbolTableBuilder {


    public static JmmSymbolTable build(JmmNode root) {

        var classDecl = root.getChildren("ClassDecl").get(0);
        String className = classDecl.get("className");
        var params = buildParams(classDecl);
        var methods = buildMethods(classDecl);
        var returnTypes = buildReturnTypes(classDecl);
        var locals = buildLocals(classDecl);
        var fields = buildFields(classDecl);
        var superClass = buildSuperClass(classDecl);
        var imports = buildImports(root);

        return new JmmSymbolTable(className, methods, returnTypes, params, locals, fields, superClass, imports);
    }

    private static List<String> buildImports(JmmNode root) {
        return root.getChildren("ImportDecl").stream().map(node -> node.get("value")).toList();
    }

    private static String buildSuperClass(JmmNode classDecl) {
        return classDecl.hasAttribute("extendClassName") ? classDecl.get("extendClassName") : null;
    }

    private static Map<String, Type> buildReturnTypes(JmmNode classDecl) {
        Map<String, Type> map = new HashMap<>();

        classDecl.getChildren("MethodDecl")
                .forEach(method -> map.put(method.get("name"), getType(method.getJmmChild(0))));

        return map;
    }

    private static Type getType(JmmNode node) {
        return switch (node.getKind()) {
            case "Integer" -> new Type("int", false);
            case "Boolean" -> new Type("boolean", false);
            case "Void" -> new Type("void", false);
            case "Array" -> new Type(getType(node.getJmmChild(0)).getName(), true);
            case "String" -> new Type("String", false);
            case "Id" -> new Type(node.get("name"), false);

            default -> throw new RuntimeException("Unknown type: " + node.getKind());
        };
    }

    private static Map<String, List<Symbol>> buildParams(JmmNode classDecl) {
        Map<String, List<Symbol>> map = new HashMap<>();

        classDecl.getChildren("MethodDecl").stream().forEach( method -> {

            List<Symbol> symbols = method.getChildren("Param").stream().map(param ->
                    new Symbol(getType(param.getJmmChild(0)), param.get("name"))
            ).toList();

            map.put(method.get("name"), symbols);
        });
        var a = classDecl.getChildren("MethodDecl");
        if (a.get(a.size() -1 ).get("hasEllipsis").equals("true")){
            map.get(a.get(a.size() -1 ).get("name")).add(new Symbol(new Type("int", true), a.get(a.size() -1 ).get("ellipsisName")));
        }

        return map;
    }


    private static List<String> buildMethods(JmmNode classDecl) {
        return classDecl.getChildren("MethodDecl").stream().map(method -> method.get("name")).toList();
    }

    private static List<Symbol> buildFields(JmmNode classDecl) {
        return classDecl.getChildren("VarDecl").stream().map(field -> new Symbol(getType(field.getJmmChild(0)), field.get("name"))).toList();
    }

    private static Map<String, List<Symbol>> buildLocals(JmmNode classDecl) {
        Map<String, List<Symbol>> map = new HashMap<>();

        classDecl.getChildren(METHOD_DECL).stream()
                .forEach(method -> map.put(method.get("name"), getLocalsList(method)));

        return map;
    }

    private static List<Symbol> getLocalsList(JmmNode methodDecl) {

        var intType = new Type(TypeUtils.getIntTypeName(), false);

        return methodDecl.getChildren(VAR_DECL).stream()
                .map(varDecl -> new Symbol(getType(varDecl.getJmmChild(0)), varDecl.get("name")))
                .toList();
    }
}
