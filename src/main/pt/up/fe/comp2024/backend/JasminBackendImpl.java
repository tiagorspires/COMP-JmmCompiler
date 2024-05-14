package pt.up.fe.comp2024.backend;

import pt.up.fe.comp.jmm.jasmin.JasminBackend;
import pt.up.fe.comp.jmm.jasmin.JasminResult;
import pt.up.fe.comp.jmm.ollir.OllirResult;
import pt.up.fe.comp.jmm.report.Report;

import java.util.ArrayList;

public class JasminBackendImpl implements JasminBackend {

    @Override
    public JasminResult toJasmin(OllirResult ollirResult) {



        return new JasminResult(ollirResult, ".class public Method\n" +
                ".super java/lang/Object\n" +
                "\n" +
                ".method public static main([Ljava/lang/String;)V\n" +
                "\n" +
                "\t.limit stack 4\n" +
                "\t.limit locals 6\n" +
                "\tbipush 10\n" +
                "\tnewarray int\n" +
                "\tastore_1\n" +
                "\ticonst_2\n" +
                "\tistore_2\n" +
                "\ticonst_1\n" +
                "\tistore_3\n" +
                "\tnew Method\n" +
                "\tastore 4\n" +
                "\taload 4\n" +
                "\tinvokespecial Method/<init>()V\n" +
                "\taload 4\n" +
                "\tiload_3\n" +
                "\tiload_2\n" +
                "\taload_1\n" +
                "\tinvokevirtual Method/foo(ZI[I)I\n" +
                "\tistore 5\n" +
                "\treturn\n" +
                ".end method\n" +
                "\n" +
                ".method public foo(ZI[I)I\n" +
                "\n" +
                "\t.limit stack 1\n" +
                "\t.limit locals 4\n" +
                "\tiload_2\n" +
                "\tireturn\n" +
                ".end method\n" +
                "\n" +
                ".method public <init>()V\n" +
                "\taload_0\n" +
                "\tinvokespecial java/lang/Object/<init>()V\n" +
                "\treturn\n" +
                ".end method", new ArrayList<Report>());
    }

}
