import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class MigrationTests {

    private final Path TEST_DATABASE_LOCAL_PATH = Paths.get("src/main/test/test.db");

    private Connection getTestDatabaseConnection() {
        try {
            return DriverManager.getConnection("jdbc:sqlite:" + TEST_DATABASE_LOCAL_PATH);
        } catch (SQLException e) {
            return null;
        }
    }


//    @Test
//    public void databaseTransactionIsEqualToSourceTransaction() {
//        Connection connection = getTestDatabaseConnection();
//        Assertions.assertNotNull(connection, "Test cannot continue, connection failed!");
//
//        UUID transactionId = UUID.fromString("32323232-0000-0000-0000-323232323232");
//        Transaction t = new Transaction(
//                TransactionType.DEPOSIT,
//                32d,
//                UUID.fromString("32323232-0000-0000-0000-323232323232"),
//                LocalDateTime.parse("10/05/2026 10:00:00", DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")),
//                TransactionCategory.CUSTOM,
//                "test32",
//                "test32",
//                transactionId
//        );
//
//        Assertions.assertTrue(DataManager.copyTransaction(t, connection), "Transaction could not be copied!");
//        try {
//            PreparedStatement stmt = connection.prepareStatement("SELECT * FROM transactions JOIN transaction_categories AS categories ON customCategoryId = categories.id WHERE uuid = ?;");
//            stmt.setString(1, transactionId.toString());
//
//            ResultSet response = stmt.executeQuery();
//
//            Transaction dbTransaction = DataManager.mapResultSetToTransaction(response);
//            assert dbTransaction.getId().equals(t.getId());
//            assert dbTransaction.getType().equals(t.getType());
//            assert dbTransaction.getAmount() == t.getAmount();
//            assert dbTransaction.getTargetAccountId().equals(t.getTargetAccountId());
//            assert dbTransaction.getTimestamp().equals(t.getTimestamp());
//            assert dbTransaction.getCategory().equals(t.getCategory());
//            assert dbTransaction.getCustomCategory().equals(t.getCustomCategory());
//            assert dbTransaction.getComment().equals(t.getComment());
//            connection.close();
//        } catch (SQLException e) {
//            System.err.println(e.getMessage());
//            e.printStackTrace();
//        }
//
//        System.out.println("Test passed, waiting for cleanup...");
//        try {
//            Thread.sleep(3000);
//        } catch (InterruptedException e) {
//            throw new RuntimeException(e);
//        }
//        cleanup();
//    }
//
//    private void cleanup() {
//        Connection connection = getTestDatabaseConnection();
//        try {
//            PreparedStatement p = connection.prepareStatement("DELETE FROM transactions; DELETE FROM transaction_categories; DELETE FROM sqlite_sequence");
//            p.execute();
//        } catch (SQLException e) {
//            System.err.println("Cleanup failed: " + e.getMessage());
//            e.printStackTrace();
//        }
//    }
}
