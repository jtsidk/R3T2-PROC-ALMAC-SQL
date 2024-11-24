package hilosBanco;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Random;

class HiloTransferencia implements Runnable {
    // Constantes que configuran la transferencia:
    private final static int DIVISOR_CANTIDAD = 50; // Reduce el rango máximo de cantidad transferida.
    private final static int ITERACIONES = 1000; // Cantidad de transferencias que realizará el hilo.

    private final Banco banco; // Referencia al banco para obtener cuentas y realizar operaciones.
    private int numHilo; // Identificador del hilo.
    private final int cantidadMáxima; // Cantidad máxima permitida por transferencia.

    // Conexión y consultas SQL específicas para este hilo.
    private Connection conexión;
    private PreparedStatement sqlMiraFondos;
    private PreparedStatement sqlRetira;
    private PreparedStatement sqlIngresa;

    // Configuraciones para el funcionamiento de las transferencias:
    static final String SQL_MIRA_FONDOS = "SELECT saldo FROM cuentas WHERE id=?";
    static final String SQL_INGRESA = "UPDATE cuentas SET saldo=saldo+? WHERE id=?";
    static final boolean RETIRA_EN_DOS_PASOS = false; // Define si se validará saldo antes de retirar.
    static final String SQL_RETIRA = RETIRA_EN_DOS_PASOS 
            ? "UPDATE cuentas set saldo=saldo-? WHERE id=?"
            : "UPDATE cuentas SET saldo=saldo-? WHERE id=? AND saldo>=?";
    static final boolean TRANSACCIÓN = true; // Activa las transacciones para mayor seguridad.
    static final boolean REORDENA_QUERIES = false; // Solo relevante en modo transacciones para evitar interbloqueos.

    // Constructor: inicializa el hilo con el banco, las cuentas y las consultas SQL preparadas.
    public HiloTransferencia(Banco b, int from, int max) throws SQLException {
        banco = b;
        numHilo = from;
        cantidadMáxima = max;

        // Establece una conexión independiente para este hilo.
        conexión = DriverManager.getConnection("jdbc:mysql://localhost/banco?allowPublicKeyRetrieval=true", "dam2",
                "asdf.1234");

        // Prepara las consultas SQL necesarias para las transferencias.
        sqlMiraFondos = conexión.prepareStatement(SQL_MIRA_FONDOS);
        sqlRetira = conexión.prepareStatement(SQL_RETIRA);
        sqlIngresa = conexión.prepareStatement(SQL_INGRESA);
    }

    // Método que ejecuta el hilo: realiza transferencias entre cuentas aleatorias.
    public void run() {
        Random rnd = new Random(); // Generador de números aleatorios para seleccionar cuentas y cantidades.
        String mensajeSalida = "Terminadas las transferencias del hilo " + numHilo;

        for (int i = 0; i < ITERACIONES; i++) {
            // Selecciona aleatoriamente cuentas de origen y destino.
            int cuentaOrigen = rnd.nextInt(banco.getNúmeroDeCuentas());
            int cuentaDestino;
            do {
                cuentaDestino = rnd.nextInt(banco.getNúmeroDeCuentas());
            } while (banco.abierto() && (cuentaDestino == cuentaOrigen)); // Asegura que las cuentas sean diferentes.

            int cantidad = rnd.nextInt(cantidadMáxima / DIVISOR_CANTIDAD); // Define una cantidad aleatoria para transferir.

            if (!banco.abierto()) {
                // Si el banco está cerrado, detiene las transferencias.
                mensajeSalida = "Saliendo por banco cerrado. Hilo " + numHilo;
                break;
            }

            // Realiza la transferencia llamando al método del banco.
            banco.transfiere(cuentaOrigen, cuentaDestino, cantidad, conexión, sqlMiraFondos, sqlRetira, sqlIngresa,
                    RETIRA_EN_DOS_PASOS, TRANSACCIÓN, REORDENA_QUERIES);
        }

        // Mensajes de finalización, indicando si se completaron o interrumpieron las transferencias.
        if (mensajeSalida.startsWith("Terminadas"))
            System.out.println(mensajeSalida);
        else
            System.err.println(mensajeSalida);

        // Cierra la conexión SQL de este hilo.
        try {
            conexión.close();
        } catch (SQLException e) {
            System.err.println(e.getMessage());
        }
    }
}
