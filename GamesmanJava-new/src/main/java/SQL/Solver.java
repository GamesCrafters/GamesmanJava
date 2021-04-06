package SQL;


import java.sql.Connection;

public class Solver {


    public static void main (String[] args) {
        Connection conn = SQLDatabaseConnection.getConnection();
        Connect4 g = new Connect4(4, 4, 4, conn);
        long start = System.currentTimeMillis();
        g.solve();
        System.out.println(System.currentTimeMillis() - start);
        //g.serialize("src/Solves/Connect4_4by4win4");
        g.play();
    }


}
