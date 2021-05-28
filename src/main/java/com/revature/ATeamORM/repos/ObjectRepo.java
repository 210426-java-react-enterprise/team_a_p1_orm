package com.revature.ATeamORM.repos;

import com.revature.ATeamORM.exceptions.DataSourceException;
import com.revature.ATeamORM.util.annotations.Entity;
import com.revature.ATeamORM.util.annotations.Id;
import com.revature.ATeamORM.util.annotations.Table;


import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.*;

import java.util.ArrayList;
import java.util.Objects;

public class ObjectRepo {

    public void create(Connection conn, Object object) {

        try{

            Class<?> clazz = Objects.requireNonNull(object.getClass());

            if (!clazz.isAnnotationPresent(Entity.class)) {
                throw new RuntimeException("This is not an entity class!");
            }

            Field[] fields = clazz.getDeclaredFields();
            Method[] methods = clazz.getDeclaredMethods();

            StringBuilder sql = new StringBuilder("insert into " + getTableName(clazz) + " (");
            int i = 1;
            for (Field field: fields) {
                field.setAccessible(true);
                sql.append(field.getName());
                if (i < fields.length) {
                    sql.append(", ");
                }
                field.setAccessible(false);
                i++;
            }
            sql.append(") values (");
            i = 1;
            for (Field field: fields) {
                field.setAccessible(true);
                String fieldCapital = field.getName().substring(0, 1).toUpperCase() + field.getName().substring(1);
                for(Method method: methods) {
                    if (method.getName().equals("get" + fieldCapital)) {
                        sql.append(method.invoke(object));
                    }
                }
                if (i < fields.length) {
                    sql.append(", ");
                }
                field.setAccessible(false);
                i++;
            }
            sql.append(")");


            PreparedStatement pstmt = conn.prepareStatement(String.valueOf(sql), new String[] { "id" });
            int rowsInserted = pstmt.executeUpdate();
            
            if (rowsInserted != 0) {
                ResultSet rs = pstmt.getGeneratedKeys();
                while (rs.next()) {
                    for(Method method: methods) {
                        if (method.getName().equals("setId")) {
                            method.invoke(object,rs.getInt("id"));
                        }
                    }
                }
            }

        } catch (SQLException e) {
            throw new DataSourceException();
        } catch (InvocationTargetException e) {
            System.out.println("Cannot invoke method");
        } catch (IllegalAccessException e) {
            System.out.println("Cannot access that object");
        }

    }
    
    
    @SuppressWarnings({"unchecked"})
    public  <T> ArrayList<T> read(Connection conn, Class<?> clazz) throws IllegalAccessException, SQLException {

        ArrayList<T> results = new ArrayList<T>();
        

        Entity entityRead = clazz.getAnnotation(Entity.class);
        String tableName = entityRead.name();
        

        Method[] methods = clazz.getDeclaredMethods();
        
        Field[] fields = clazz.getDeclaredFields();

        String wantedNames = "";
        String wantedValues = "";
        
        for(Field field: fields){
            field.setAccessible(true);
            String tmp = field.getName();
            if(!tmp.equals("") || !tmp.equals("0")){
                wantedNames += field.getName() +", ";
                wantedValues += tmp + ", ";
            }
            
            field.setAccessible(false);
        }
    
        String[] namesArray = wantedNames.substring(0,wantedNames.length()-1)
                                      .split(",");
        
        String[] valueArray = wantedValues.substring(0,wantedNames.length()-1)
                                    .split(",");
        
        String sql = "select * from "+ tableName + " where " + namesArray[0] +"="+ valueArray[0];
        
        Statement pstmt = conn.createStatement();
        ResultSet resultSet = pstmt.executeQuery(sql);
        
        
       /* try {

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
        }*/

        return results;

    }

    public void update(Connection conn, Object object) {

        try {

            Class<?> clazz = Objects.requireNonNull(object.getClass());

            if (!clazz.isAnnotationPresent(Entity.class)) {
                throw new RuntimeException("This is not an entity class!");
            }

            Field[] fields = clazz.getDeclaredFields();
            Method[] methods = clazz.getDeclaredMethods();

            StringBuilder sql = new StringBuilder("update "  + getTableName(clazz) + " set ");
            int i = 1;
            for (Field field: fields) {
                field.setAccessible(true);
                String fieldCapital = field.getName().substring(0, 1).toUpperCase() + field.getName().substring(1);
                for (Method method: methods) {
                    if (method.getName().equals("get" + fieldCapital)) {
                        sql.append(field.getName())
                                .append(" = ")
                                .append(method.invoke(object));
                    }
                }
                if (i < fields.length) {
                    sql.append(", ");
                }
                field.setAccessible(false);
                i++;
            }
            sql.append(" where id = ");
            for (Method method: methods) {
                if (method.getName().equals("getId")) {
                    sql.append(method.invoke(object));
                }
            }

            PreparedStatement pstmt = conn.prepareStatement(String.valueOf(sql));

            int rowsUpdated = pstmt.executeUpdate();

            if (rowsUpdated != 0) {
                ResultSet rs = pstmt.getGeneratedKeys();
                while (rs.next()) {
                    for (Method method: methods) {
                        if (method.getName().equals("setId")) {
                            method.invoke(object, rs.getInt("id"));
                        }
                    }
                }
            }

        } catch (InvocationTargetException e) {
            System.out.println("Cannot invoke that method");
        } catch (IllegalAccessException e) {
            System.out.println("Cannot access that object");
        } catch (SQLException e) {
            throw new DataSourceException();
        }
    }

    public void delete(Connection conn, Object object) {

        try {

            Class<?> clazz = Objects.requireNonNull(object.getClass());

            if (!clazz.isAnnotationPresent(Entity.class)) {
                throw new RuntimeException("This is not an entity class!");
            }

            Method method = clazz.getMethod("getId");

            String sql = "delete from " + getTableName(clazz) + " where id = " + method.invoke(object);

            PreparedStatement pstmt = conn.prepareStatement(sql);

            pstmt.executeUpdate();

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
    
//    //Connection conn,
//    public void sqlUpdateQuery( Object o) throws IllegalAccessException, SQLException {
//        Class<?> clazz = Objects.requireNonNull(o.getClass());
//
//        if (!clazz.isAnnotationPresent(Entity.class)) {
//            throw new RuntimeException("This is not an entity class!");
//        }
//
//
//        //Get name of entity, next we would check if entity(name) is empty, if so
//        //then we use name of class.
//        Entity anoEntity = clazz.getAnnotation(Entity.class);
//        String tableName = anoEntity.name();
//        String nameOfClass = clazz.getSimpleName();
//
//        String sqlUpdater = "update " + tableName + " set ";//column = value
//        String values = "";
//        String fieldNames = "";
//        String tmpId = "";
//
//
//        //get the fields to be put into the sql statement
//        Field[] oClassFields = clazz.getDeclaredFields();
//        for (Field field : oClassFields) {
//            field.setAccessible(true);
//
//            if (field.isAnnotationPresent(Id.class)) {
//                tmpId = field.get(o).toString();
//            }else if (!field.isAnnotationPresent(Id.class)){
//                System.out.println("Field name is==> " + field.getName() + " with value is==> " + field.get(o).toString());
//                //System.out.println(values);
//                System.out.println(sqlUpdater);
//                System.out.println();
//                values = (String) field.get(o).toString() + ",";
//                fieldNames = field.getName();
//                sqlUpdater += fieldNames + " = " + values ;
//
//            }
//
//
//            field.setAccessible(false);
//        }
//        sqlUpdater = sqlUpdater.substring(0,sqlUpdater.length()-1);
//        sqlUpdater += " where id = " + tmpId;
//        System.out.println(sqlUpdater);
//        PreparedStatement pstmt = null;
//
//       // pstmt = conn.prepareStatement(sqlUpdater);
//        //pstmt.executeUpdate();
//
//
//
//    }
    
   /* public void sqlDelete(Object o, Connection conn) {
        Class<?> clazz = Objects.requireNonNull(o.getClass());
        
        if (!clazz.isAnnotationPresent(Entity.class)) {
            throw new RuntimeException("Not an entity type.");
        }
        
        //get name of table which is in entity name or if empty use class name
        Entity anoEntity = clazz.getAnnotation(Entity.class);
        String tableName = anoEntity.name();
        
        String sql = "delete from " + tableName + " where ";
        
        Field[] oClassFields = clazz.getDeclaredFields();
        for (Field field : oClassFields) {
            field.setAccessible(true);
            if (field.getAnnotation()){
            
            }
        }
    }*/

    private String getTableName(Class<?> clazz) {
        String tableName = clazz.getAnnotation(Table.class).name();
        if (tableName.equals("")) {
            tableName = clazz.getName();
        }
        return tableName;
    }
}
