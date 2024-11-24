package hilosBanco;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class Banco {
	// Variables principales que definen el estado y la configuración inicial del
	// banco.
	private final int saldoInicial; // Saldo inicial por cuenta.
	private final int númeroDeCuentas; // Número total de cuentas en el banco.
	private boolean abierto; // Estado del banco: abierto o cerrado.
	private Connection conexión; // Conexión a la base de datos.

	// Constructor del banco: inicializa el estado y las cuentas en la base de
	// datos.
	public Banco(int numCuentas, int saldoInicial) {
		this.abierto = true;
		this.saldoInicial = saldoInicial;
		this.númeroDeCuentas = numCuentas;

		try {
			// Establece conexión con la base de datos y prepara las tablas.
			conexión = DriverManager.getConnection("jdbc:mysql://localhost/banco?allowPublicKeyRetrieval=true", "dam2",
					"asdf.1234");

			// Reinicia la tabla de cuentas y las inicializa con saldos iguales.
			Statement sql = conexión.createStatement();
			sql.execute("DROP TABLE IF EXISTS cuentas ");
			sql.execute("create table cuentas(id int primary key, saldo float)");
			for (int i = 0; i < numCuentas; i++) {
				sql.execute(String.format("insert into cuentas values(%d,%d)", i, saldoInicial));
			}
		} catch (SQLException e) {
			// Si ocurre un error, el banco se considera cerrado.
			System.err.println(e.getMessage());
			conexión = null;
			this.abierto = false;
		}
	}

	// Método principal para realizar transferencias entre cuentas.
	public void transfiere(int origen, int destino, int cantidad, Connection conexiónHilo,
			PreparedStatement sqlMiraFondos, PreparedStatement sqlRetira, PreparedStatement sqlIngresa,
			boolean retiraEnDosPasos, boolean transacción, boolean reordena) {

		try {
			// Si está habilitada, comienza una transacción.
			if (transacción)
				conexiónHilo.setAutoCommit(false);

			// Configura las consultas SQL con los parámetros de la transferencia.
			sqlMiraFondos.setInt(1, origen);
			sqlRetira.setFloat(1, cantidad);
			sqlRetira.setInt(2, origen);
			if (!retiraEnDosPasos)
				sqlRetira.setFloat(3, cantidad);
			sqlIngresa.setFloat(1, cantidad);
			sqlIngresa.setInt(2, destino);

			boolean faltaSaldo = true; // Indica si hay suficiente saldo para la transferencia.

			// Lógica para manejar la transferencia según las configuraciones.
			if (retiraEnDosPasos) {
				ResultSet res = sqlMiraFondos.executeQuery();
				if (res.next() && res.getFloat(1) >= cantidad) {
					// Realiza las operaciones en orden normal o invertido según la configuración.
					if (origen < destino || !transacción || !reordena) {
						sqlRetira.executeUpdate();
						sqlIngresa.executeUpdate();
					} else {
						sqlIngresa.executeUpdate();
						sqlRetira.executeUpdate();
					}
					faltaSaldo = false;
				}
			} else {
				// Retira e ingresa directamente si hay saldo suficiente.
				if (sqlRetira.executeUpdate() > 0) {
					sqlIngresa.executeUpdate();
					faltaSaldo = false;
				}
			}

			if (faltaSaldo) {
				// Notifica si no hay fondos suficientes para la transferencia.
				System.err.printf("No puedo tranferir %d de %d a %d por falta de fondos\n", cantidad, origen, destino);
			}

			// Finaliza la transacción si está activa.
			if (transacción)
				conexiónHilo.commit();
		} catch (SQLException e) {
			// Maneja errores SQL y realiza un rollback en caso de fallos.
			System.err.println("Problema SQL " + e.getMessage());
			try {
				conexiónHilo.rollback();
			} catch (SQLException e1) {
				System.err.println("Problema haciendo rollback " + e.getMessage());
			}
		}

		// Restaura el modo autocommit tras la transacción.
		if (transacción) {
			try {
				conexiónHilo.setAutoCommit(true);
			} catch (SQLException e) {
				System.err.println("Problema haciendo autocommit " + e.getMessage());
			}
		}

		// Verifica que no haya saldos negativos tras la operación.
		try {
			ResultSet res2 = sqlMiraFondos.executeQuery();
			if (!res2.next() || res2.getFloat(1) < 0) {
				System.err.println("Descubierto en cuenta " + origen + " saldo: " + res2.getFloat(1));
			}
		} catch (SQLException e) {
			System.err.println("Problema SQL bis " + e.getMessage());
		}
	}

	// Método para verificar el balance total y detectar cuentas en descubierto.
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

		// Identifica cuentas con saldo negativo.
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
	}

	// Métodos auxiliares para manejar el estado del banco.
	public int getNúmeroDeCuentas() {
		return númeroDeCuentas;
	}

	boolean abierto() {
		return abierto;
	}

	void cierraBanco() {
		abierto = false;
	}

	// Cierra la conexion con la base de datos, si hay error lo indica
	void cierraConexiónBD() {
		try {
			conexión.close();
		} catch (SQLException e) {
			System.err.println("Error cerrando conexión de BBDD " + e.getMessage());
		}
	}
}
