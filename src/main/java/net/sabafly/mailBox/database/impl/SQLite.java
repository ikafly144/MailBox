package net.sabafly.mailBox.database.impl;

import net.sabafly.mailBox.MailBox;
import net.sabafly.mailBox.database.Database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class SQLite extends Base implements Database {

    private Connection connection;

    @Override
    public Connection getConnection() {
        try {
            return DriverManager.getConnection(String.format("jdbc:sqlite:%s/data.db", MailBox.getInstance().getDataFolder()));
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void setup() {
        try {
            Class.forName("org.sqlite.JDBC");
            //noinspection ResultOfMethodCallIgnored
            MailBox.getInstance().getDataFolder().mkdirs();
            this.connection = DriverManager.getConnection(String.format("jdbc:sqlite:%s/data.db", MailBox.getInstance().getDataFolder()));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        super.setup();
    }

    @Override
    public void close() {
        try {
            connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
