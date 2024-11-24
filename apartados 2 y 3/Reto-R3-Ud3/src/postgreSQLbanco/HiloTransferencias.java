package postgreSQLbanco;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Random;

class HiloTransferencia implements Runnable {
    private final static int DIVISOR_CANTIDAD = 50; // Para dividir la cantidad inicial para el tope por transferencia
    private final static int ITERACIONES = 1000;
    private final Banco banco;
    private final int numHilo;
    private final int cantidadMáxima;

    // Conexión única para cada hilo
    private Connection conexión;

    public HiloTransferencia(Banco b, int from, int max) throws SQLException {
        banco = b;
        numHilo = from;
        cantidadMáxima = max;

        // Configurar la conexión a PostgreSQL
        conexión = DriverManager.getConnection("jdbc:postgresql://localhost:5432/adat1", "dam1", "asdf.1234");
    }

    public void run() {
        Random rnd = new Random();
        String mensajeSalida = "Terminadas las transferencias del hilo " + numHilo;

        for (int i = 0; i < ITERACIONES; i++) {
            // Elegir aleatoriamente cuentas de origen y destino
            int cuentaOrigen, cuentaDestino;
            cuentaOrigen = rnd.nextInt(banco.getNúmeroDeCuentas());
            do {
                cuentaDestino = rnd.nextInt(banco.getNúmeroDeCuentas());
            } while (banco.abierto() && (cuentaDestino == cuentaOrigen));

            int cantidad = rnd.nextInt(cantidadMáxima / DIVISOR_CANTIDAD);

            if (!banco.abierto()) {
                mensajeSalida = "Saliendo por banco cerrado. Hilo " + numHilo;
                break;
            }

            banco.transfiere(cuentaOrigen, cuentaDestino, cantidad, conexión);
        }

        // Mensaje final del hilo
        if (mensajeSalida.startsWith("Terminadas")) {
            System.out.println(mensajeSalida);
        } else {
            System.err.println(mensajeSalida);
        }

        // Cerrar la conexión del hilo
        try {
            if (conexión != null && !conexión.isClosed()) {
                conexión.close();
            }
        } catch (SQLException e) {
            System.err.println("Error cerrando la conexión del hilo " + numHilo + ": " + e.getMessage());
        }
    }
}
