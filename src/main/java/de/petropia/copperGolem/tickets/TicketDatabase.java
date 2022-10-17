package de.petropia.copperGolem.tickets;

import com.mysql.cj.jdbc.MysqlDataSource;
import de.petropia.copperGolem.CopperGolem;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ForkJoinPool;

public class TicketDatabase {
    private static DataSource dataSource;
    private static final String CREATE_TABLE = "CREATE TABLE IF NOT EXISTS tickets (" +
            "UserID VARCHAR(20) PRIMARY KEY, " +    //Discord user id
            "TicketChannelID VARCHAR(20) NOT NULL, " +  //Discord channel id
            "Description TEXT NOT NULL, " +  //Subject of Ticket
            "Title TEXT NOT NULL," +    //Title of Ticket
            "ClaimedBy VARCHAR(20)," + //Discord user id of supporter who claimed ticket
            "Status VARCHAR(20) NOT NULL" + //TicketStatus of the ticket
            ");";
    private static final String HAS_USER_OPEN_TICKET = "SELECT * FROM tickets WHERE UserID = ?";
    private static final String ADD_TICKET = "INSERT INTO tickets (UserID, TicketChannelID, Description, Title, ClaimedBy, Status) VALUES (?, ?, ?, ?, NULL, ?)";
    private static final String CLAIM_TICKET = "UPDATE tickets SET ClaimedBy = ?, Status = '"+ TicketStatus.CLAIMED.name() +"' WHERE TicketChannelID = ?";
    private static final String GET_CLAIMED_BY = "SELECT ClaimedBy FROM tickets WHERE TicketChannelID = ?";
    private static final String DELETE_TICKET = "DELETE FROM tickets WHERE TicketChannelID = ?";
    public static boolean connect(){
        Properties properties = CopperGolem.getInstance().getProperties();
        String password = properties.getProperty("DatabasePassword");
        String user = properties.getProperty("DatabaseUser");
        String database = properties.getProperty("DatabaseName");
        String address = properties.getProperty("DatabaseAddress");
        String port = properties.getProperty("DatabasePort");

        MysqlDataSource dataSource = new MysqlDataSource();
        dataSource.setServerName(address);
        dataSource.setPort(Integer.parseInt(port));
        dataSource.setDatabaseName(database);
        dataSource.setUser(user);
        dataSource.setPassword(password);
        try(Connection connection = dataSource.getConnection()) {
            if(!connection.isValid(1000)){
                System.out.println("Can't establish database connection");
                return false;
            }
            System.out.println("Connected to DB Server sucessfully!");
            dataSource.setAutoReconnect(true);
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
        TicketDatabase.dataSource = dataSource;
        return true;
    }

    public static void createTablesIfNotExist(){
        try(Connection connection = dataSource.getConnection(); PreparedStatement statement = connection.prepareStatement(CREATE_TABLE)) {
            statement.execute();
        } catch (SQLException e) {
            System.out.println("Cant create Table!");
            throw new RuntimeException(e);
        }
    }

    public static CompletableFuture<Boolean> hasUserOpenTicket(String userId){
        return CompletableFuture.supplyAsync(() -> {
            try(Connection connection = dataSource.getConnection(); PreparedStatement statement = connection.prepareStatement(HAS_USER_OPEN_TICKET)){
                statement.setString(1, userId);
                ResultSet resultSet = statement.executeQuery();
                boolean returnValue = resultSet.next();
                resultSet.close();
                return returnValue;
            } catch (SQLException e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        });
    }

    public static void createTicket(String userID, String ticketChannelID, String title, String description){
        ForkJoinPool.commonPool().execute(() -> {
            try(Connection connection = dataSource.getConnection(); PreparedStatement statement = connection.prepareStatement(ADD_TICKET)){
                statement.setString(1, userID);
                statement.setString(2, ticketChannelID);
                statement.setString(3, description);
                statement.setString(4, title);
                statement.setString(5, TicketStatus.UNCLAIMED.name());
                statement.execute();
            } catch (SQLException e){
                e.printStackTrace();
            }
        });
    }

    public static CompletableFuture<String> getClaimedBy(String ticketChannelID){
        return CompletableFuture.supplyAsync(() -> {
            try(Connection connection = dataSource.getConnection(); PreparedStatement statement = connection.prepareStatement(GET_CLAIMED_BY)){
                statement.setString(1, ticketChannelID);
                ResultSet resultSet = statement.executeQuery();
                resultSet.next();
                String result = resultSet.getString(1);
                resultSet.close();
                return result;
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public static void setTicketClaimed(String ticketChannelID, String claimedUserID){
        new Thread(() -> {
            try(Connection connection = dataSource.getConnection(); PreparedStatement statement = connection.prepareStatement(CLAIM_TICKET)){
                statement.setString(1, claimedUserID);
                statement.setString(2, ticketChannelID);
                statement.execute();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }).start();
    }

    public static void deleteTicket(String ticketChannelID){
        new Thread(() -> {
            try(Connection connection = dataSource.getConnection(); PreparedStatement statement = connection.prepareStatement(DELETE_TICKET)){
                statement.setString(1, ticketChannelID);
                statement.execute();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }).start();
    }
}
