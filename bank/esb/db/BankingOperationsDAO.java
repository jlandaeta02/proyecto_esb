package com.bank.esb.db;

import org.json.JSONObject;
import com.bank.esb.config.AppConfig;

import java.math.BigDecimal;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.random.RandomGenerator;
//import com.ibm.as400.access.AS400JDBCPreparedStatement;

/**
 * Acceso a Datos nativo en DB2 for i (OS/400).
 */
public class BankingOperationsDAO {

    public static String DesencriptaDAO(String apiKey)  {
        String masterKey = AppConfig.getInstance().getSystemMasterKey();
        String sql = "SELECT DECRYPT_CHAR(SECRET_KEY_ENC, '" + masterKey + "') AS SECRET_KEY_DESC FROM JLANDAETA1.API_KEYS WHERE API_KEY = ? AND ESTADO = 'A'";

        try (Connection conn = HikariDbPool.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            //pstmt.setString(1, masterKey.trim());
            pstmt.setString(1, apiKey);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("SECRET_KEY_DESC").trim();
                }
            }
        } catch (SQLException e) {
            System.err.println("[DAO] Error consultando Desencriptado en DB2 for i: " + e.getMessage());
        }

        return null; // Cliente no encontrado o inactivo

    }

    /**
     * Obtiene el Secret encriptado en AES asignado a un cliente según su API Key.
     */
    public static String getClientSecretByApiKey(String apiKey) {
        String masterKey = AppConfig.getInstance().getSystemMasterKey();
        //System.err.println("masterKey : " + masterKey);
        String sql = "SELECT HEX(TRIM(SECRET_KEY_ENC)) AS SECRET_KEY_ENC FROM JLANDAETA1.API_KEYS WHERE API_KEY = ? AND ESTADO = 'A'";
        //sql = "SELECT DECRYPT_CHAR(SECRET_KEY_ENC, 'ClaveMaestraSistemaIBM') AS SECRET_KEY_DES  FROM JLANDAETA1.API_KEYS WHERE API_KEY = ? AND ESTADO = 'A'";

        try (Connection conn = HikariDbPool.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, apiKey);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    //return rs.getString("SECRET_KEY_DES").trim();
                    return rs.getString("SECRET_KEY_ENC").trim();
                }
            }
        } catch (SQLException e) {
            System.err.println("[DAO] Error consultando API Key en DB2 for i: " + e.getMessage());
        }

        return null; // Cliente no encontrado o inactivo
    }

    /**
     * Consulta el saldo en línea de una cuenta bancaria en las tablas del AS/400.
     */
    public static JSONObject getAccountBalance(String accountNumber) {
        JSONObject result = new JSONObject();
        //String sql = "SELECT ACC_NUM, ACC_TYPE, ACC_BAL, ACC_STAT FROM JLANDAETA1.ACCOUNTS WHERE ACC_NUM = ?";
        String sql = "CALL JLANDAETA1.SP_GET_ACCOUNT_DETAILS( ?, ?, ? )";

        try (Connection conn = HikariDbPool.getConnection();
            CallableStatement pstmt = conn.prepareCall(sql)){
             //PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, accountNumber);
            pstmt.registerOutParameter(2, Types.VARCHAR);
            pstmt.registerOutParameter(3, Types.VARCHAR);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    String status = rs.getString("ACC_STAT").trim();
                    
                    if (!"ACTIVE".equalsIgnoreCase(status)) {
                        result.put("code", "403");
                        result.put("message", "La cuenta especificada no se encuentra activa.");
                        return result;
                    }
                    String resCode = pstmt.getString(2);
                    String resMessage = pstmt.getString(3);

                    //result.put("code", "00");
                    result.put("code", resCode);
                    //result.put("message", "Consulta exitosa.");
                    result.put("message", resMessage);

                    result.put("cuenta", rs.getString("ACC_NUM").trim());
                    result.put("tipoCuenta", rs.getString("ACC_TYPE").trim());
                    
                    //result.put("saldoDisponible", rs.getBigDecimal("ACC_BAL"));
                    BigDecimal saldo = rs.getBigDecimal("ACC_BAL");
                    if (saldo != null) {
                        // Definir el formato deseado (Ejemplo: 1.500,00)
                        DecimalFormatSymbols simbolos = new DecimalFormatSymbols(new Locale("es", "VE")); // o "es", "ES"
                        simbolos.setGroupingSeparator('.'); // Puntos para miles
                        simbolos.setDecimalSeparator(',');  // Coma para decimales

                        DecimalFormat df = new DecimalFormat("#,##0.00", simbolos);
                        String saldoFormateado = df.format(saldo); // Resultado: "1.500,00"

                        result.put("saldoDisponible", saldoFormateado);
                    } else {
                        result.put("saldoDisponible", "0,00");
                    }
                } else {
                    result.put("code", "404");
                    result.put("message", "Cuenta no encontrada.");
                }
            }
        } catch (SQLException e) {
            System.err.println("[DAO] Error en getAccountBalance: " + e.getMessage());
            result.put("code", "500");
            result.put("message", "Error de base de datos al consultar el saldo.");
        }

        return result;
    }

     

    /**
     * Ejecuta una transferencia invocando un Stored Procedure en DB2 for i (ej. SP_TRANSFER)
     * asegurando consistencia transaccional y bloqueos nativos de registro.
     */
    public static JSONObject executeTransfer(String origen, String destino, BigDecimal monto) {
        JSONObject result = new JSONObject();
        // Llamada a Stored Procedure SQL/ILE RPG en el AS/400: (ORIGEN, DESTINO, MONTO, OUT_CODE, OUT_MSG, OUT_REF)
        String sql = "{CALL JLANDAETA1.SP_TRANSFER_FUNDS(?, ?, ?, ?, ?, ?)}";
        //sql = "UPDATE JLANDAETA1.ACCOUNTS SET ACC_BAL = ACC_BAL - " + monto + " WHERE ACC_NUM = ?";
        //System.err.println("sql : " + sql);
        String resCode = "00"; 
        String refNum = "0";
        try (Connection conn = HikariDbPool.getConnection();
            //PreparedStatement pstmt = conn.prepareStatement(sql)) {
             CallableStatement pstmt = conn.prepareCall(sql)) {

          // Parametros JL      
              //cstmt.setBigDecimal(1, monto);
              pstmt.setString(1, origen);
              pstmt.setString(2, destino);
              pstmt.setBigDecimal(3, monto);
              pstmt.registerOutParameter(4, Types.VARCHAR);
              pstmt.registerOutParameter(5, Types.VARCHAR);
              pstmt.registerOutParameter(6, Types.VARCHAR);

              //int filasAfectadas = pstmt.executeUpdate();
              pstmt.execute();

              resCode = pstmt.getString(4);
              String resMessage = pstmt.getString(5);
              refNum  = pstmt.getString(6);

              result.put("code", resCode);
              result.put("message", resMessage);
              result.put("referencia", refNum != null ? refNum.trim() : "");


              //System.err.println("Filas afectadas por update : " + filasAfectadas);
            /*if (filasAfectadas > 0)
             {     
                String sql2 = "UPDATE JLANDAETA1.ACCOUNTS SET ACC_BAL = ACC_BAL + " + monto + " WHERE ACC_NUM = ?";
                Connection conn2 = HikariDbPool.getConnection();
                CallableStatement cstmt2 = conn2.prepareCall(sql2); 

            // Parametros JL      
                //cstmt2.setBigDecimal(1, monto);
                cstmt2.setString(1, destino);
                //System.err.println("sql 2 : " + sql2);
                cstmt2.execute();

                RandomGenerator rng = RandomGenerator.getDefault();
                int numeroInt = rng.nextInt(31000);
                refNum = String.valueOf(numeroInt); // numeroInt.toString();
                resCode = "00"; 
             } else {
                    resCode = "01"; 
                    result.put("code", "404");
                    result.put("message", "Cuenta de origen no encontrada ");
                }
          // Parámetros IN
         //   cstmt.setString(1, origen);
         //   cstmt.setString(2, destino);
            //cstmt.setBigDecimal(3, monto);

            // Parámetros OUT
            //cstmt.registerOutParameter(4, Types.VARCHAR); // Código Respuesta (ej: "00", "51")
            //cstmt.registerOutParameter(5, Types.VARCHAR); // Mensaje
            //cstmt.registerOutParameter(6, Types.VARCHAR); // Número de Referencia Único

            //cstmt.execute();

            //String resCode = cstmt.getString(4);
            //String resMsg = cstmt.getString(5);
            //String refNum = cstmt.getString(6);

            //result.put("code", resCode != null ? resCode.trim() : "99");
            //result.put("message", resMsg != null ? resMsg.trim() : "Sin respuesta del procedimiento.");
            
            if ("00".equals(resCode)) {
                result.put("referencia", refNum != null ? refNum.trim() : "");
            }
                */

        } catch (SQLException e) {
            System.err.println("[DAO] Error ejecutando Stored Procedure de Transferencia: " + e.getMessage());
            result.put("code", "500");
            result.put("message", "Error transaccional en el núcleo bancario: " + e.getMessage());
        }

        return result;
    }
}