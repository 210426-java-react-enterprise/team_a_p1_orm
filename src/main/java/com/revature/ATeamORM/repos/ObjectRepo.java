package com.revature.ATeamORM.repos;

import com.revature.ATeamORM.exceptions.DataSourceException;
import com.revature.ATeamORM.util.Entity;
import com.revature.ATeamORM.util.Id;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Objects;

public class ObjectRepo {
    
    public void save(Connection conn, Object object, String fromObject, String tableName, ArrayList<String> columns) {
        
        object.getClass();
        
        String[] objects = fromObject.split(",");
        
        try {
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
            
            PreparedStatement pstmt = conn.prepareStatement(String.valueOf(sql), new String[]{"user_id"});
            int rowsInserted = pstmt.executeUpdate();
            
            if (rowsInserted != 0) {
                ResultSet rs = pstmt.getGeneratedKeys();
                while (rs.next()) {
                    //                   object.setId(rs.getInt("user_id"));
                }
            }
            
        } catch (SQLException e) {
            throw new DataSourceException();
        }
        
    }
    
    
    
}
