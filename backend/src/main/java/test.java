import java.sql.*;

public class test {
    public static void main(String[] args) {
        String url = "jdbc:mysql://localhost:3306/nomnom";
        String user = "root";
        String password = "自己的数据库密码"; // 这里一定要改成你真实的密码

        try (Connection conn = DriverManager.getConnection(url, user, password)) {
            System.out.println("数据库连接成功！正在读取菜单...");
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT name FROM menu");

            while (rs.next()) {
                System.out.println("菜名: " + rs.getString("name"));
            }
            // 测试插入一个新品
            String insertSql = "INSERT INTO menu (name, price, category) VALUES ('黄金脆皮猪蹄', 58.0, '热菜')";
            int rows = stmt.executeUpdate(insertSql);
            System.out.println("成功添加了 " + rows + " 道新菜！");

        } catch (SQLException e) {
            e.printStackTrace();


        }
    }
}
