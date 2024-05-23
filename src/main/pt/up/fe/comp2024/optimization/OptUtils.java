package pt.up.fe.comp2024.optimization;

import org.specs.comp.ollir.Instruction;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp2024.ast.NodeUtils;
import pt.up.fe.comp2024.symboltable.JmmSymbolTableBuilder;
import pt.up.fe.specs.util.exceptions.NotImplementedException;

import java.util.List;
import java.util.Optional;

import static pt.up.fe.comp2024.ast.Kind.TYPE;

public class OptUtils {
    private static int tempNumber = -1;

    private static int ifNumber = -1;

    private static int whileNumber = -1;

    private static int conditionalNumber = -1;

    public static String getTemp() {

        return getTemp("tmp");
    }

    public static String getTemp(String prefix) {

        return prefix + getNextTempNum();
    }

    public static int getNextTempNum() {

        tempNumber += 1;
        return tempNumber;
    }

    public static int getNextIfNum() {

        ifNumber += 1;
        return ifNumber;
    }

    public static int getNextWhileNum() {

        whileNumber += 1;
        return whileNumber;
    }

    public static int getNextCondNumber() {

        conditionalNumber += 1;
        return conditionalNumber;
    }

    public static String toOllirType(JmmNode typeNode) {

        TYPE.checkOrThrow(typeNode);

        String typeName = typeNode.get("name");

        return toOllirType(typeName);
    }

    public static String toOllirType(Type type) {
        StringBuilder code = new StringBuilder();
        if(type.isArray()) {
            code.append(".array");
        }
        code.append(toOllirType(type.getName()));
        return code.toString();
    }

    private static String toOllirType(String typeName) {
        String type = "." + switch (typeName) {
            case "int" -> "i32";
            //case "bool" -> "bool";
            case "boolean" -> "bool";
            case "String" -> "String";
            case "void" -> "V";
            default -> typeName;
        };

        return type;
    }


}
