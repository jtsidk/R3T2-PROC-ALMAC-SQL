package hilosBanco;

import java.sql.SQLException;

public class SimulaBanco {

    // Configuración del sistema bancario simulado.
    public static final int NÚMERO_DE_CUENTAS = 10; // Total de cuentas en el banco.
    public static final int NÚMERO_DE_HILOS = 20;  // Total de hilos (simulaciones de usuarios).
    public static final int SALDO_INICIAL = 10000; // Saldo inicial de cada cuenta.

    public static void main(String[] args) throws InterruptedException, SQLException {
        // Se inicializa el banco con las cuentas configuradas.
        Banco banco = new Banco(NÚMERO_DE_CUENTAS, SALDO_INICIAL);

        // Crea y lanza múltiples hilos para simular transferencias simultáneas.
        Thread[] hilos = new Thread[NÚMERO_DE_HILOS];
        for (int i = 0; i < NÚMERO_DE_HILOS; i++) {
            hilos[i] = new Thread(new HiloTransferencia(banco, i, SALDO_INICIAL)); // Cada hilo representa un usuario.
            hilos[i].start(); // Inicia el hilo.
        }

        // Espera hasta que todos los hilos finalicen su ejecución.
        int numHilos = NÚMERO_DE_HILOS;
        while (numHilos > 0) {
            // Comprueba periódicamente el estado del banco para detectar problemas o inconsistencias.
            banco.comprueba();
            Thread.sleep(1000); // Pausa entre comprobaciones.

            // Cuenta cuántos hilos siguen activos.
            numHilos = 0;
            for (int i = 0; i < NÚMERO_DE_HILOS; i++) {
                if (hilos[i].isAlive()) { // Verifica si el hilo está aún en ejecución.
                    numHilos++;
                }
            }
            System.out.println("Hilos vivos: " + numHilos);

            // Si solo queda un hilo, se cierra el banco para evitar bloqueos.
            if (numHilos < 2) {
                banco.cierraBanco();
            }
        }

        System.out.println("Terminadas todas las transferencias");

        // Realiza una comprobación final de la consistencia de las cuentas.
        banco.comprueba();

        // Cierra la conexión a la base de datos al finalizar todas las operaciones.
        banco.cierraConexiónBD();
    }
}
