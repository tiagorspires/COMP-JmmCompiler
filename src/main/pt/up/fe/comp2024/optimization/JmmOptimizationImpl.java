package pt.up.fe.comp2024.optimization;

import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.comp.jmm.ollir.JmmOptimization;
import pt.up.fe.comp.jmm.ollir.OllirResult;

import java.util.Collections;

public class JmmOptimizationImpl implements JmmOptimization {

    @Override
    public OllirResult toOllir(JmmSemanticsResult semanticsResult) {



        return new OllirResult(semanticsResult, "myClass {\n" +
                "    .construct myClass().V {\n" +
                "   \t invokespecial(this, \"<init>\").V;\n" +
                "    }\n" +
                "    \n" +
                "    .method public sum(A.array.i32, B.array.i32).array.i32 {\n" +
                "   \t t1.i32 :=.i32 arraylength($1.A.array.i32).i32;\n" +
                "   \t C.array.i32 :=.array.i32 new(array, t1.i32).array.i32;\n" +
                "   \t i.i32 :=.i32 0.i32;\n" +
                "   \t \n" +
                "   \t Loop:\n" +
                "   \t\t t1.i32 :=.i32 arraylength($1.A.array.i32).i32;\n" +
                "   \t\t if (i.i32 >=.bool t1.i32) goto End;\n" +
                "   \t\t \n" +
                "   \t\t t2.i32 :=.i32 $1.A[i.i32].i32;\n" +
                "   \t\t t3.i32 :=.i32 $2.B[i.i32].i32;\n" +
                "   \t\t t4.i32 :=.i32 t2.i32 +.i32 t3.i32;\n" +
                "   \t\t C[i.i32].i32 :=.i32 t4.i32;\n" +
                "   \t\t i.i32 :=.i32 i.i32 +.i32 1.i32;\n" +
                "   \t\t goto Loop;\n" +
                "   \t End:\n" +
                "   \t\t ret.array.i32 C.array.i32;\n" +
                "    }\n" +
                "}\n" +
                "\n", Collections.emptyList());
    }

    @Override
    public OllirResult optimize(OllirResult ollirResult) {

        //TODO: Do your OLLIR-based optimizations here

        return ollirResult;
    }
}
