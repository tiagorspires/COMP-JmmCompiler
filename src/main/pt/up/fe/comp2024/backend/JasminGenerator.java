package pt.up.fe.comp2024.backend;

import org.specs.comp.ollir.*;
import org.specs.comp.ollir.tree.TreeNode;
import pt.up.fe.comp.jmm.ollir.OllirResult;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.specs.util.classmap.FunctionClassMap;
import pt.up.fe.specs.util.exceptions.NotImplementedException;
import pt.up.fe.specs.util.utilities.StringLines;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Generates Jasmin code from an OllirResult.
 * <p>
 * One JasminGenerator instance per OllirResult.
 */
public class JasminGenerator {

    private static final String NL = "\n";
    private static final String TAB = "   ";

    private final OllirResult ollirResult;

    List<Report> reports;

    String code;
    Method currentMethod;

    private final FunctionClassMap<TreeNode, String> generators;

    public JasminGenerator(OllirResult ollirResult) {
        this.ollirResult = ollirResult;

        reports = new ArrayList<>();
        code = null;
        currentMethod = null;

        this.generators = new FunctionClassMap<>();
        generators.put(ClassUnit.class, this::generateClassUnit);

        generators.put(Method.class, this::generateMethod);
        generators.put(GetFieldInstruction.class, this::getFieldInstruction);
        generators.put(PutFieldInstruction.class, this::putFieldInstruction);
        generators.put(CallInstruction.class, this::callInstruction);
        generators.put(AssignInstruction.class, this::generateAssign);
        generators.put(SingleOpInstruction.class, this::generateSingleOp);
        generators.put(LiteralElement.class, this::generateLiteral);
        generators.put(Operand.class, this::generateOperand);
        generators.put(BinaryOpInstruction.class, this::generateBinaryOp);
        generators.put(ReturnInstruction.class, this::generateReturn);
    }

    private String callInstruction(CallInstruction callInstruction) {
        var code = new StringBuilder();
        String returnType= this.getType(callInstruction.getReturnType());
        var arguments = new StringBuilder();
        if(!(callInstruction.getArguments().isEmpty())) {
            for (int i = 0; i < callInstruction.getArguments().size(); i++) {
                arguments.append(this.getType(callInstruction.getArguments().get(i).getType()));
                code.append(this.generateOperand((Operand) callInstruction.getArguments().get(i)));
            }
        }
        switch(callInstruction.getInvocationType().toString()){
            case "NEW":
                code.append("new ").append(((ClassType)callInstruction.getCaller().getType()).getName()).append(NL);
                code.append("dup").append(NL);
                return code.toString();
            case "invokevirtual":
                code.append("invokevirtual ").append(((ClassType)callInstruction.getCaller().getType()).getName());
                code.append("/").append((((LiteralElement)callInstruction.getMethodName()).getLiteral()).substring(1, (((LiteralElement)callInstruction.getMethodName()).getLiteral()).length()-1)).append("(").append(arguments).append(")").append(returnType).append(NL);
                return code.toString();
            case "invokespecial":
                code.append("invokespecial ").append(((ClassType)callInstruction.getCaller().getType()).getName()).append("/").append((((LiteralElement)callInstruction.getMethodName()).getLiteral()).substring(1, (((LiteralElement)callInstruction.getMethodName()).getLiteral()).length()-1)).append("(").append(arguments).append(")").append(returnType).append(NL);
                code.append("pop").append(NL);
                return code.toString();
            case "invokestatic":
                code.append("invokestatic ").append(((Operand)callInstruction.getCaller()).getName()).append("/").append((((LiteralElement)callInstruction.getMethodName()).getLiteral()).substring(1, (((LiteralElement)callInstruction.getMethodName()).getLiteral()).length()-1));
                code.append("(").append(arguments).append(")").append(returnType).append(NL);
                return code.toString();
            case "invokeinterface":
                code.append("invokeinterface ").append(((ClassType)callInstruction.getCaller().getType()).getName());
                code.append("/").append((((LiteralElement)callInstruction.getMethodName()).getLiteral()).substring(1, (((LiteralElement)callInstruction.getMethodName()).getLiteral()).length()-1)).append("(").append(arguments).append(")").append(returnType).append(NL);
                return code.toString();
        }

        return code.toString();
    }

    private String getType(Type type){
        String a= "";
        switch (type.toString()){
            case "INT32":
                a= "I";
                break;
            case "VOID":
                a= "V";
                break;
            case "BOOLEAN":
                a= "Z";
                break;
            case "STRING":
                a= "Ljava/lang/String;";
                break;
            case "THIS":
                a= "T";
                break;
            case "OBJECTREF":
                a="OBJ";
                break;
            case "CLASS":
                a= "C";
                break;
            default:
                a= "V";
                break;
        }
        return a;
    }

    private String putFieldInstruction(PutFieldInstruction putFieldInstruction) {
        var code = new StringBuilder();
        String type = "";
        type = this.getType(putFieldInstruction.getField().getType());
        code.append("aload 0").append(NL).append(generators.apply(putFieldInstruction.getValue())).append("putfield ").append(ollirResult.getOllirClass().getClassName()).append("/").append(putFieldInstruction.getField().getName()).append(" ").append(type).append(NL);
        return code.toString();
    }

    private String getFieldInstruction(GetFieldInstruction getFieldInstruction) {
        var code = new StringBuilder();
        String type = "";
        type = this.getType(getFieldInstruction.getField().getType());
        code.append("aload 0").append(NL).append("getfield ").append(ollirResult.getOllirClass().getClassName()).append("/").append(getFieldInstruction.getField().getName()).append(" ").append(type).append(NL);
        return code.toString();
    }

    public List<Report> getReports() {
        return reports;
    }

    public String build() {

        // This way, build is idempotent
        if (code == null) {
            code = generators.apply(ollirResult.getOllirClass());
        }

        return code;
    }


    private String generateClassUnit(ClassUnit classUnit) {

        var code = new StringBuilder();

        // generate class name
        var className = ollirResult.getOllirClass().getClassName();
        String isFinal = "";
        String isStatic = "";

        if(ollirResult.getOllirClass().isFinalClass()){
            isFinal = "final ";
        }
        if(ollirResult.getOllirClass().isStaticClass()){
            isStatic = "static ";
        }
        code.append(".class ").append("public ").append(isStatic).append(isFinal).append(className).append(NL).append(NL);

        // TODO: Hardcoded to Object, needs to be expanded
        var superName = ollirResult.getOllirClass().getSuperClass();
        if (!(superName== null)) {

            code.append(".super ").append(superName).append(NL).append(NL);
        }
        else{
            code.append(".super java/lang/Object").append(NL).append(NL);
        }

        for(var field : ollirResult.getOllirClass().getFields()){
            if(field.isFinalField()){
                String type = "";
                type = this.getType(field.getFieldType());
                code.append(".field ").append("final ").append(field.getFieldName()).append(" ").append(type).append(NL);

            }
            else if(field.isStaticField()){
                String type = "";
                type = this.getType(field.getFieldType());
                code.append(".field ").append("static ").append(field.getFieldName()).append(" ").append(type).append(NL);
            }
            else{
                String type = "";
                type = this.getType(field.getFieldType());
                code.append(".field ").append("public ").append(field.getFieldName()).append(" ").append(type).append(NL);
            }
        }
        code.append(NL);

        // generate a single constructor method
        var constructor = new StringBuilder();
        if (!(superName== null)){
            constructor.append(".method public <init>()V").append(NL).append(TAB).append("aload_0").append(NL).append(TAB).append("invokespecial ").append(superName).append("/<init>()V").append(NL).append(TAB).append("return").append(NL).append(".end method");
            code.append(constructor);
        }
        else {
            var defaultConstructor = """
                    ;default constructor
                    .method public <init>()V
                        aload_0
                        invokespecial java/lang/Object/<init>()V
                        return
                    .end method
                    """;
            code.append(defaultConstructor);
        }
        // generate code for all other methods
        for (var method : ollirResult.getOllirClass().getMethods()) {

            // Ignore constructor, since there is always one constructor
            // that receives no arguments, and has been already added
            // previously
            if (method.isConstructMethod()) {
                continue;
            }

            code.append(generators.apply(method));
        }

        return code.toString();
    }


    private String generateMethod(Method method) {

        // set method
        currentMethod = method;

        var code = new StringBuilder();
        var mod = new StringBuilder();
        if(method.isFinalMethod()){
            mod.append("final ");
        }
        if(method.isStaticMethod()){
            mod.append("static ");
        }
        // calculate modifier
        var modifier = method.getMethodAccessModifier() != AccessModifier.DEFAULT ?
                method.getMethodAccessModifier().name().toLowerCase() + " " :
                "";

        var methodName = method.getMethodName();
        String returnType= this.getType(method.getReturnType());

        code.append("\n.method ").append(modifier).append(mod).append(methodName).append("(").append(generateParam(method)).append(")").append(returnType).append(NL);

        // Add limits
        code.append(TAB).append(".limit stack 99").append(NL);
        code.append(TAB).append(".limit locals 99").append(NL);
        code.append(TAB).append("aload 0").append(NL);


        for (var inst : method.getInstructions()) {
            var instCode = StringLines.getLines(generators.apply(inst)).stream()
                    .collect(Collectors.joining(NL + TAB, TAB, NL));

            code.append(instCode);
        }

        code.append(".end method\n");

        // unset method
        currentMethod = null;
        return code.toString();
    }

    private String generateAssign(AssignInstruction assign) {
        var code = new StringBuilder();

        // generate code for loading what's on the right
        code.append(generators.apply(assign.getRhs()));

        // store value in the stack in destination
        var lhs = assign.getDest();

        if (!(lhs instanceof Operand)) {
            throw new NotImplementedException(lhs.getClass());
        }

        var operand = (Operand) lhs;
        // get register
        var reg = currentMethod.getVarTable().get(operand.getName()).getVirtualReg();
        var storeInstruction = new StringBuilder();
        ElementType elementType = assign.getTypeOfAssign().getTypeOfElement();
        switch (elementType) {
            case INT32:
            case BOOLEAN:
                    storeInstruction.append("istore ").append(reg).append(NL);

                break;

            case OBJECTREF:
            case STRING:
                storeInstruction.append("astore ").append(reg).append(NL);
                storeInstruction.append("aload ").append(reg).append(NL);
                break;

            case THIS:
                storeInstruction.append("aload ").append(reg).append(NL);
                break;

            default:
                storeInstruction.append("UNSUPPORTED TYPE ").append(elementType);
        }

        code.append(storeInstruction).append(NL);
        return code.toString();
    }

    private String generateSingleOp(SingleOpInstruction singleOp) {
        return generators.apply(singleOp.getSingleOperand());
    }

    private String generateLiteral(LiteralElement literal) {
        return "ldc " + literal.getLiteral() + NL;
    }

    private String generateOperand(Operand operand) {
        // get register
        var reg = currentMethod.getVarTable().get(operand.getName()).getVirtualReg();
        if(operand.getType().getTypeOfElement().toString().equals("OBJECTREF")){
            return "aload " + reg + NL;
        }
        return "iload " + reg + NL;
    }

    private String generateBinaryOp(BinaryOpInstruction binaryOp) {
        var code = new StringBuilder();

        // load values on the left and on the right
        code.append(generators.apply(binaryOp.getLeftOperand()));
        code.append(generators.apply(binaryOp.getRightOperand()));

        // apply operation
        var op = switch (binaryOp.getOperation().getOpType()) {
            case ADD -> "iadd";
            case MUL -> "imul";
            default -> throw new NotImplementedException(binaryOp.getOperation().getOpType());
        };

        code.append(op).append(NL);

        return code.toString();
    }

    private String generateReturn(ReturnInstruction returnInst) {
        var code = new StringBuilder();

        // TODO: Hardcoded to int return type, needs to be expanded
        if(!(returnInst.getReturnType().toString().equals("VOID"))){
            code.append(generators.apply(returnInst.getOperand()));
        }


        switch (returnInst.getReturnType().toString()){
            case "INT32":
                code.append("ireturn").append(NL);
                break;
            case "BOOLEAN":
                code.append("ireturn").append(NL);
                break;
            case "VOID":
                code.append("return").append(NL);
                break;
            case "STRING":
                code.append("areturn").append(NL);
                break;
            case "OBJECTREF":
                code.append("areturn").append(NL);
        }

        return code.toString();
    }

    public String generateParam(Method method){
        var code = new StringBuilder();
        for (var param : method.getParams()) {
            switch (param.getType().toString()) {
                case "INT32":
                    code.append("I");
                    break;
                case "BOOLEAN":
                    code.append("Z");
                    break;
                case "STRING[]":
                    code.append("[Ljava/lang/String;");
                    break;
                case "CLASS":
                    code.append("C");
                    break;
                case "THIS":
                    code.append("T");
                    break;
                case "OBJECTREF":
                    code.append("OBJ");
                    break;
                default:
                    // Handle other types if needed
                    break;
            }
        }
        return code.toString();
    }

}
