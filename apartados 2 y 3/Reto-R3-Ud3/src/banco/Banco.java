package banco;

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
			conexión = DriverManager.getConnection("jdbc:mysql://localhost/adat1?allowPublicKeyRetrieval=true", "dam1",
					"asdf.1234");

			// Inicializa la base de datos de cuentas:
			Statement sql = conexión.createStatement();
			sql.execute("DROP TABLE IF EXISTS cuentas ");
			sql.execute("create table cuentas(id int primary key, saldo float)");
			for (int i = 0; i < numCuentas; i++) {
				sql.execute(String.format("insert into cuentas values(%d,%d)", i, saldoInicial));
			}
		} catch (SQLException e) {
			System.err.println(e.getMessage());
			conexión = null;
			this.abierto = false;
		}
	}

	public void transfiere(int origen, int destino, int cantidad, Connection conexiónHilo) {
	    try {
	        // llamar al procedimiento almacenado
	    	
	        CallableStatement cs = conexiónHilo.prepareCall("{CALL TransferirFondos(?, ?, ?)}");
	        cs.setInt(1, origen);  // Parametro origen
	        cs.setInt(2, destino); // parametro destino
	        cs.setFloat(3, cantidad); // parametro cantidad
	        cs.execute(); // ejecutar procedimiento
	        
	        System.out.printf("Transferencia de %d de %d a %d completada.\n", cantidad, origen, destino);
	    } catch (SQLException e) {
	    	
	        // manejar errores de la base de datos
	        System.err.println("Error en transferencia: " + e.getMessage());
	    }
	}



	public void comprueba() throws SQLException {
		int saldoTotal = 0;
		Statement sql = conexión.createStatement();
		ResultSet res = sql.executeQuery("SELECT SUM(saldo) FROM cuentas");
		if (res.next()) {
			saldoTotal = (int) res.getFloat(1);
			if (saldoTotal != (númeroDeCuentas * saldoInicial)) {
				System.err.println("¡¡¡¡¡No cuadran las cuentas!!!! saldo total " + saldoTotal);
			} else {
				System.out.println("Balance correcto");
			}
		}
		
		res = sql.executeQuery("SELECT id FROM cuentas WHERE saldo<0");
		while (res.next()) {
			System.err.println("DESCUBIERTO en cuenta " + res.getInt(1));
		}
		

		/*
		 * Detallando por cuenta: ResultSet res =
		 * sql.executeQuery("SELECT id,saldo FROM cuentas"); while (res.next()) { int
		 * saldo = (int) res.getFloat(2); saldoTotal += saldo;
		 * System.out.printf("Cuenta %d , saldo %d, parcial %d\n", res.getInt(1),
		 * saldo,* saldoTotal); }
		 */

	} // comprueba

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
			conexión.close();
		} catch (SQLException e) {
			System.err.println("Error cerrando conexión de BBDD " + e.getMessage());
		}
	}
}