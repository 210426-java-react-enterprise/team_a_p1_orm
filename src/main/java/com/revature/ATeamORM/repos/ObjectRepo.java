package com.revature.ATeamORM.repos;

import com.revature.ATeamORM.exceptions.DataSourceException;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

public class ObjectRepo {

    public void create(Connection conn, Object object, String tableName, Map<String, String> columns) {

        try{

            Class theClass = object.getClass();
            Method method = theClass.getMethod("setId",Integer.class);

            StringBuilder sql = new StringBuilder("insert into " + tableName + " (");
            int i = 1;
            for (String column: columns.keySet()) {
                sql.append(column);
                if (i < columns.size()) {
                    sql.append(", ");
                }
                i++;
            }
            sql.append(") values (");
            i = 1;
            for (String value: columns.values()) {
                sql.append(value);
                i++;
                if (i < columns.size()) {
                    sql.append(", ");
                }
            }
            sql.append(")");

            PreparedStatement pstmt = conn.prepareStatement(String.valueOf(sql), new String[] { "id" });
            int rowsInserted = pstmt.executeUpdate();

            if (rowsInserted != 0) {
                ResultSet rs = pstmt.getGeneratedKeys();
                while (rs.next()) {
                    method.invoke(object,rs.getInt("id"));
                }
            }

        } catch (SQLException e) {
            throw new DataSourceException();
        } catch (InvocationTargetException e) {
            System.out.println("Cannot invoke method");
        } catch (IllegalAccessException e) {
            System.out.println("Cannot access that object");
        } catch (NoSuchMethodException e) {
            System.out.println("No such method exists");
        }

    }

    public ResultSet read(Connection conn, String table, Map<String, String> columns) {

        ResultSet rs = null;

        try {

            StringBuilder sql = new StringBuilder("select * from " + table + " where ");
            int i = 1;
            for (String column: columns.keySet()) {
                sql.append(column + " = " + columns.get(column));
                if (i < columns.size()) {
                    sql.append(" and ");
                }
                i++;
            }

            if(conn == null) {
                throw new NullPointerException(System.getProperty("host_url") +
                        " is what has been given as the host url from environment variables \n and the username is: "+
                        System.getProperty("db_username") + " with a password of: "+System.getProperty("db_password"));
            }

            PreparedStatement pstmt = conn.prepareStatement(String.valueOf(sql));

            rs = pstmt.executeQuery();

        } catch (SQLException e) {
            throw new DataSourceException();
        }

        return rs;

    }

    public void update(Connection conn, Object object, String table, Map<String, String> columns) {

        try {

            Class theClass = object.getClass();
            Method method = theClass.getMethod("getId",Integer.class);
            Method otherMethod = theClass.getMethod("setId",Integer.class);

            StringBuilder sql = new StringBuilder("update "  + table + " set ");
            int i = 1;
            for (String column: columns.keySet()) {
                sql.append(column + " = " + columns.get(column));
                if (i < columns.size()) {
                    sql.append(", ");
                }
            }
            sql.append(" where id = " + method.invoke(object));

            PreparedStatement pstmt = conn.prepareStatement(String.valueOf(sql));

            int rowsUpdated = pstmt.executeUpdate();

            if (rowsUpdated != 0) {
                ResultSet rs = pstmt.getGeneratedKeys();
                while (rs.next()) {
                    otherMethod.invoke(object, rs.getInt("id"));
                }
            }

        } catch (NoSuchMethodException e) {
            System.out.println("No such method exists");
        } catch (InvocationTargetException e) {
            System.out.println("Cannot invoke that method");
        } catch (IllegalAccessException e) {
            System.out.println("Cannot access that object");
        } catch (SQLException e) {
            throw new DataSourceException();
        }
    }

    public void delete(Connection conn, Object object, String table) {

        try {

            Class theClass = object.getClass();
            Method method = theClass.getMethod("getId",Integer.class);

            String sql = "delete from " + table + " where id = " + method.invoke(object);

            PreparedStatement pstmt = conn.prepareStatement(sql);

            int rowsUpdated = pstmt.executeUpdate();

        } catch (NoSuchMethodException e) {
            System.out.println("No such method exists");
        } catch (InvocationTargetException e) {
            System.out.println("Cannot invoke that method");
        } catch (IllegalAccessException e) {
            System.out.println("Cannot access that object");
        } catch (SQLException e) {
            throw new DataSourceException();
        }
    }

}
