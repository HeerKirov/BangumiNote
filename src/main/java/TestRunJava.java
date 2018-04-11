public class TestRunJava {
    public static void main(String[] args) {
        String s = "";
        String[] s2 = s.split(",");
        for(String i : s2) {
            System.out.println("\"" + i + "\"");
        }
    }
}
