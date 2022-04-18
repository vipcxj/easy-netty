package io.github.vipcxj.easynetty.utils;

public class Constants {

    public static final boolean JAVA9 = testJava9();

    private static boolean testJava9() {
        try {
            Class.forName("java.lang.invoke.VarHandle");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}
