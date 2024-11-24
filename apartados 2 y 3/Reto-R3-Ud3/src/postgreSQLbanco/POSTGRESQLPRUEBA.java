package postgreSQLbanco;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class POSTGRESQLPRUEBA {
    public static void main(String[] args) {

        String url = "jdbc:postgresql://localhost:5432/adat1";
        String user = "dam1";
        String password = "asdf.1234";

      
        try (Connection connection = DriverManager.getConnection(url, user, password)) {
            if (connection != null) {
                System.out.println("conexion exitoso a PostgreSQL!");
            } else {
                System.out.println("conexion falló.");
            }
        } catch (SQLException e) {
            System.err.println("Error al conectarse: " + e.getMessage());
        }
    }
}