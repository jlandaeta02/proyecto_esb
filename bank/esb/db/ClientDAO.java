package com.bank.esb.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class ClientDAO {

    public static ClientCredentials getClientCredentials(String apiKey) throws Exception {
        String sql = "SELECT CLIENT_ID, API_KEY, SECRET_KEY_AES FROM ESBLIB.SYS_CLIENTS WHERE API_KEY = ? AND STATUS = 'A'";

        // Se solicita la conexión directamente al Pool HikariCP
        try (Connection conn = HikariDbPool.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, apiKey);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new ClientCredentials(
                        rs.getString("CLIENT_ID"),
                        rs.getString("API_KEY"),
                        rs.getString("SECRET_KEY_AES")
                    );
                }
            }
        }
        return null;
    }

    public record ClientCredentials(String clientId, String apiKey, String encryptedSecretKey) {}
}
