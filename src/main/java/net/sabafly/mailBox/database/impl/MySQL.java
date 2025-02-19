package net.sabafly.mailBox.database.impl;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import net.sabafly.mailBox.MailBox;
import net.sabafly.mailBox.database.Database;

import java.sql.Connection;
import java.sql.SQLException;

public class MySQL extends Base implements Database {

    private final HikariConfig config = new HikariConfig();
    private HikariDataSource dataSource;

    @Override
    public Connection getConnection() {
        try {
            return dataSource.getConnection();
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public void setup() {
        config.setPoolName("MailBox-Pool");

        config.setMaximumPoolSize(10);
        config.setMinimumIdle(5);
        config.setMaxLifetime(1800000);
        config.setKeepaliveTime(60000);
        config.setConnectionTimeout(5000);

        config.setDriverClassName("com.mysql.jdbc.Driver");
        config.setJdbcUrl("jdbc:mysql://" + MailBox.config().database.host + ":" + MailBox.config().database.port + "/" + MailBox.config().database.database + "?useSSL=false");
        config.addDataSourceProperty("user", MailBox.config().database.username);
        config.addDataSourceProperty("password", MailBox.config().database.password);

        dataSource = new HikariDataSource(config);

        super.setup();
    }

    @Override
    public void close() {
        try {
            dataSource.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
