package postgreSQLbanco;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class Banco {
    private final int saldoInicial;
    private final int númeroDeCuentas;
    private boolean abierto;
    private Connection conexión;

    public Banco(int numCuentas, int saldoInicial) {
        this.abierto = true;
        this.saldoInicial = saldoInicial;
        this.númeroDeCuentas = numCuentas;

        try {
            conexión = DriverManager.getConnection("jdbc:postgresql://localhost:5432/adat1", "dam1", "asdf.1234");
            Statement sql = conexión.createStatement();
            sql.execute("DROP TABLE IF EXISTS cuentas");
            sql.execute("CREATE TABLE cuentas (id INT PRIMARY KEY, saldo NUMERIC)");
            for (int i = 0; i < numCuentas; i++) {
                sql.execute(String.format("INSERT INTO cuentas VALUES (%d, %d)", i, saldoInicial));
            }
        } catch (SQLException e) {
            System.err.println(e.getMessage());
            conexión = null;
            this.abierto = false;
        }
    }

    public void transfiere(int origen, int destino, int cantidad, Connection conexiónHilo) {
        try {
            // Llamar al procedimiento almacenado usando CALL
            String sql = "CALL TransferirFondos(?, ?, ?)";
            CallableStatement cs = conexiónHilo.prepareCall(sql);

            cs.setInt(1, origen); // ID de cuenta origen
            cs.setInt(2, destino); // ID de cuenta destino
            cs.setBigDecimal(3, new java.math.BigDecimal(cantidad)); // Monto a transferir

            cs.execute(); // Ejecutar el procedimiento
            System.out.printf("Transferencia de %d de %d a %d completada.\n", cantidad, origen, destino);
        } catch (SQLException e) {
            // Manejar errores de la base de datos
            System.err.println("Error en transferencia: " + e.getMessage());
        }
    }


    public void comprueba() throws SQLException {
        int saldoTotal = 0;
        Statement sql = conexión.createStatement();
        ResultSet res = sql.executeQuery("SELECT SUM(saldo) FROM cuentas");
        if (res.next()) {
            saldoTotal = res.getInt(1);
            if (saldoTotal != (númeroDeCuentas * saldoInicial)) {
                System.err.println("¡¡¡¡¡No cuadran las cuentas!!!! Saldo total " + saldoTotal);
            } else {
                System.out.println("Balance correcto");
            }
        }

        res = sql.executeQuery("SELECT id FROM cuentas WHERE saldo < 0");
        while (res.next()) {
            System.err.println("DESCUBIERTO en cuenta " + res.getInt(1));
        }
    }

    public int getNúmeroDeCuentas() {
        return númeroDeCuentas;
    }

    boolean abierto() {
        return abierto;
    }

    void cierraBanco() {
        abierto = false;
    }

    void cierraConexiónBD() {
        try {
            if (conexión != null) {
                conexión.close();
            }
        } catch (SQLException e) {
            System.err.println("Error cerrando conexión de BBDD " + e.getMessage());
        }
    }

}