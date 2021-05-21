package com.revature.ATeamORM.repos;

import com.revature.ATeamORM.exceptions.DataSourceException;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

public class ObjectRepo {

    public void save(Connection conn, Object object, String fromObject, String tableName, ArrayList<String> columns) throws NoSuchMethodException {

        Class theClass = object.getClass();
        Method method = theClass.getMethod("setId",Integer.class);

        String[] objects = fromObject.split(",");

        try{
            StringBuilder sql = new StringBuilder("insert into " + tableName + " (");
            for (int i = 0; i < columns.size(); i++) {
                sql.append(columns.get(i));
                if (i < columns.size() - 1) {
                    sql.append(", ");
                }
            }
            sql.append(") values (");
            for (int i = 0; i < objects.length; i++) {
                sql.append(objects[i]);
                if (i < columns.size() - 1) {
                    sql.append(", ");
                }
            }
            sql.append(")");

            PreparedStatement pstmt = conn.prepareStatement(String.valueOf(sql), new String[] { "user_id" });
            int rowsInserted = pstmt.executeUpdate();

            if (rowsInserted != 0) {
                ResultSet rs = pstmt.getGeneratedKeys();
                while (rs.next()) {
                    method.invoke(object,rs.getInt("user_id"));
                }
            }

        } catch (SQLException e) {
            throw new DataSourceException();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }

    }
}
