package com.revature.ATeamORM.repos;

import com.revature.ATeamORM.annotations.Column;
import com.revature.ATeamORM.annotations.Id;
import com.revature.ATeamORM.annotations.Table;
import com.revature.ATeamORM.datasource.Result;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import com.revature.ATeamORM.annotations.Entity;
import java.sql.*;
import java.util.*;

public class ObjectRepo {
    
    public void create(Connection conn, Object object) throws SQLException {
        
        try{
            Class<?> oClass = Objects.requireNonNull(object.getClass());
            
            // All classes passed in must be annotated with @Entity
            if (!oClass.isAnnotationPresent(Entity.class)) {
                throw new RuntimeException("This is not an entity class!");
            }
            
            // Filters through fields in class for ones annotated with @Column but without the @Id annotation
            Field[] fields = Arrays.stream(oClass.getDeclaredFields())
                                   .filter(f -> f.isAnnotationPresent(Column.class) && !f.isAnnotationPresent(Id.class))
                                   .toArray(Field[]::new);
            
            StringBuilder sql = new StringBuilder("insert into " + getTableName(oClass) + " (");
            int i = 1;
            // Gets the column_name and appends it to the sql string for each field annotated with @Column
            for (Field field: fields) {
                field.setAccessible(true);
                sql.append(getColumnName(field));
                if (i < fields.length) {
                    sql.append(", ");
                }
                field.setAccessible(false);
                i++;
            }
            sql.append(") values (");
            i = 1;
            
            // Goes through each @Column annotated field in class and appends sql string with the value of the field
            for (Field field: fields) {
                field.setAccessible(true);
                sql.append(encapsulateString(field.get(object)));
                if (i < fields.length) {
                    sql.append(", ");
                }
                field.setAccessible(false);
                i++;
            }
            sql.append(")");
            
            Field field = Arrays.stream(oClass.getDeclaredFields()).filter(f -> f.isAnnotationPresent(Id.class)).findFirst().get();
            field.setAccessible(true);
            String fieldId = getColumnName(field);
            field.setAccessible(false);
            // Puts sql string into a prepared statement, executes it, retrieves the id, then inserts new id back into object
            PreparedStatement pstmt = conn.prepareStatement(sql.toString(), new String[] { fieldId });
            if (pstmt.executeUpdate() != 0) {
                ResultSet rs = pstmt.getGeneratedKeys();
                while (rs.next()) {
                    field.setAccessible(true);
                    setObjectValues(rs, field, object, fieldId);
                    field.setAccessible(false);
                }
            }
        } catch (IllegalAccessException e) {
            System.out.println("Cannot access that object");
            e.printStackTrace();
        } catch (NoSuchElementException e) {
            System.out.println("Could not find @Id annotation in class");
            e.printStackTrace();
        }
        
    }
    
    @SuppressWarnings({"unchecked"})
    public <T> Result<T> read(Connection conn, Class<T> clazz, String fieldName, String fieldValue) throws SQLException {
        
        List<T> objectList = new ArrayList<>();
        
        // All classes passed in must be annotated with @Entity
        if (!clazz.isAnnotationPresent(Entity.class)) {
            throw new RuntimeException("This is not an entity class!");
        }
        
        // Filters through fields in class for ones annotated with @Column
        Field[] fields = Arrays.stream(clazz.getDeclaredFields())
                               .filter(f -> f.isAnnotationPresent(Column.class))
                               .toArray(Field[]::new);
        
        StringBuilder sql = new StringBuilder("select * from ").append(getTableName(clazz))
                                                               .append(" where ");
        
        try {
            Field field = clazz.getDeclaredField(fieldName);
            sql.append(getColumnName(field))
               .append(" = ")
               .append(encapsulateString(fieldValue));
            
            Statement pstmt = conn.createStatement();
            ResultSet rs = pstmt.executeQuery(sql.toString());
            
            while(rs.next()) {
                Constructor<?> objectConstructor = clazz.getConstructor();
                T object = (T) Objects.requireNonNull(objectConstructor.newInstance());
                for (Field f : fields) {
                    setObjectValues(rs, f, object, getColumnName(f));
                }
                objectList.add(object);
                
            }
        } catch (NoSuchMethodException e) {
            System.out.println("Constructor does not exist!");
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            System.out.println("Cannot invoke constructor!");
            e.printStackTrace();
        } catch (InstantiationException e) {
            System.out.println("Cannot instantiate object!");
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            System.out.println("Constructor is not public!");
            e.printStackTrace();
        } catch (NoSuchFieldException e) {
            System.out.println("Could not find {fieldName} in your {Class<?>}!");
            e.printStackTrace();
        }
        
        return new Result<>(objectList);
    }
    
    public void update(Connection conn, Object object) throws SQLException {
        
        try {
            Class<?> oClass = Objects.requireNonNull(object.getClass());
            
            // All classes passed in must be annotated with @Entity
            if (!oClass.isAnnotationPresent(Entity.class)) {
                throw new RuntimeException("This is not an entity class!");
            }
            
            // Filters through fields in class for ones annotated with @Column
            Field[] fields = Arrays.stream(oClass.getDeclaredFields())
                                   .filter(f -> f.isAnnotationPresent(Column.class))
                                   .toArray(Field[]::new);
            
            // Used to create a camelCase string for the getter method that will be invoked
            StringBuilder sql = new StringBuilder("update "  + getTableName(oClass) + " set ");
            int i = 1;
            
            // Goes through each @Column annotated field in class and appends sql string with "column_name = fieldValue,"
            for (Field field: fields) {
                field.setAccessible(true);
                sql.append(getColumnName(field))
                   .append(" = ")
                   .append(encapsulateString(field.get(object)));
                if (i < fields.length) {
                    sql.append(", ");
                }
                field.setAccessible(false);
                i++;
            }
            sql.append(" where ");
            
            String idName = "";
            // Finds the field annotated with @Id and gets its field name to build a query of "where id_name = id"
            for (Field field : fields) {
                field.setAccessible(true);
                if (field.isAnnotationPresent(Id.class)) {
                    sql.append(getColumnName(field))
                       .append(" = ")
                       .append(field.get(object));
                    break;
                }
                field.setAccessible(false);
            }
            
            // Loads sql string into a PreparedStatement and executes it, updating the database
            // Final sql string will look something like this:
            // update tableName set user_id = object.id, username = object.username, password = object.password where user_id = object.id
            PreparedStatement pstmt = conn.prepareStatement(sql.toString());
            System.out.println(sql);
            pstmt.executeUpdate();
            
        } catch (IllegalAccessException e) {
            System.out.println("Cannot access that object");
            e.printStackTrace();
        }
    }
    
    public void delete(Connection conn, Object object) throws SQLException {
        
        try {
            Class<?> oClass = Objects.requireNonNull(object.getClass());
            
            // All classes passed in must be annotated with @Entity
            if (!oClass.isAnnotationPresent(Entity.class)) {
                throw new RuntimeException("This is not an entity class!");
            }
            
            // Filters through fields in class for one annotated with @Column and @Id (there should only be one)
            Field field = Arrays.stream(oClass.getDeclaredFields())
                                .filter(f -> f.isAnnotationPresent(Column.class) && f.isAnnotationPresent(Id.class))
                                .findFirst().get();
            
            // Finds and prepares to invoke the getter method for the field annotated with @Column and @Id
            field.setAccessible(true);
            String sql = "delete from " + getTableName(oClass) + " where " + getColumnName(field) + " = " + field.get(object);
            field.setAccessible(false);
            
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.executeUpdate();
            
        } catch (IllegalAccessException e) {
            System.out.println("Cannot access that object");
            e.printStackTrace();
        } catch (NoSuchElementException e) {
            System.out.println("Missing @Id annotation");
            e.printStackTrace();
        }
    }
    
    private String getTableName(Class<?> clazz) {
        String tableName = clazz.getAnnotation(Table.class).name();
        if (tableName.equals("")) {
            tableName = clazz.getName();
        }
        return tableName;
    }
    
    private String getColumnName(Field field) {
        String columnName = field.getAnnotation(Column.class).name();
        if (columnName.equals("")) {
            columnName = field.getName();
        }
        return columnName;
    }
    
    private <T> String encapsulateString (T t) {
        StringBuilder s = new StringBuilder();
        if (t.getClass().getSimpleName().equals("String")) {
            s.append("\'")
             .append(t)
             .append("\'");
        } else {
            s.append(t);
        }
        return s.toString();
    }
    
    private <T> void setObjectValues(ResultSet rs, Field field, T object, String dbID) throws SQLException, IllegalAccessException {
        field.setAccessible(true);
        switch (field.getType().getSimpleName()) {
            case ("String"):
                field.set(object, rs.getString(dbID));
                break;
            case ("int"):
            case ("Integer"):
                field.set(object, rs.getInt(dbID));
                break;
            case ("double"):
            case ("Double"):
                field.set(object, rs.getDouble(dbID));
                break;
            case ("float"):
            case ("Float"):
                field.set(object, rs.getFloat(dbID));
                break;
            case ("boolean"):
                field.set(object, rs.getBoolean(dbID));
                break;
        }
        field.setAccessible(false);
        
    }
    
    //WORKING update method!
    
    /**
     * Method for being able to to update a entry using Id. In other words the DB that will be references
     * must have Id column and POJO must also have a ID column.
     *
     * @param conn Connection to DB using JDBC.Please reference ConnectionFactory.
     * @param o    Object with Entity anotation, else we throw RunTimeException
     * @throws IllegalAccessException
     * @throws SQLException
     */
    public void sqlUpdateQuery(Connection conn, Object o) throws IllegalAccessException, SQLException {
        Class<?> oClass = Objects.requireNonNull(o.getClass());
        
        if (!oClass.isAnnotationPresent(Entity.class)) {
            throw new RuntimeException("This is not an entity class!");
        }
        
        
        //Get name of entity, next we would check if entity(name) is empty, if so
        //then we use name of class.
        Entity anoEntity = oClass.getAnnotation(Entity.class);
        String tableName = anoEntity.name();
        
        String sqlUpdater = "update " + "public." + tableName + " set ";//column = value
        String fieldNames = "";
        String fieldValue="";
        String tmpIdName = "";
        String tmpIdvalue ="";
        //first loop through to prepare statement
        //get the fields to be put into the sql statement
        Field[] oClassFields = oClass.getDeclaredFields();
        for (int i = 0; i < oClassFields.length; i++) {
            Field field = oClassFields[i];
            field.setAccessible(true);
            if (field.isAnnotationPresent(Id.class)) {
                Column cName = field.getAnnotation(Column.class);
                tmpIdName = cName.name();
                tmpIdvalue = field.get(o).toString();
            } else if (field.get(o) instanceof Integer) {
                
                //System.out.println(values);
                Column columnNames = field.getAnnotation(Column.class);
                fieldNames = columnNames.name();
                fieldValue = field.get(o).toString();
                sqlUpdater += fieldNames + " = " + fieldValue +",";
                
            }else{
                Column columnNames = field.getAnnotation(Column.class);
                fieldNames = columnNames.name();
                fieldValue = field.get(o).toString();
                sqlUpdater += fieldNames + " = \'" + fieldValue +"\',";
                
            }
            field.setAccessible(false);
        }
        sqlUpdater = sqlUpdater.substring(0, sqlUpdater.length() - 1);
        sqlUpdater += " where " + tmpIdName + "= "+ tmpIdvalue;
        System.out.println(sqlUpdater);
        
        PreparedStatement pstmt = conn.prepareStatement(sqlUpdater);
        pstmt.executeUpdate();
      /*  String values = "";
        Integer tmpId = 0;
        
        for (int i = 1; i < oClassFields.length + 1; i++) {
            Field field = oClassFields[i - 1];
            field.setAccessible(true);
            
            if (field.get(o) instanceof Integer) {
                if (field.isAnnotationPresent(Id.class)) {
                    tmpId = (Integer) field.get(o);
                } else {
                    pstmt.setInt(i - 1, (Integer) field.get(o));
                }
            } else if (field.get(o) instanceof String) {
                pstmt.setString(i - 1, (String) field.get(o));
            }
            field.setAccessible(false);
        }
        //pstmt.setInt(oClassFields.length, tmpId);
       // pstmt.executeUpdate();
        */
    }
    
    
    /**
     * Method for deleting an entity type that references an entry in a DB using the Id column from
     * the application onto id from DB.
     * @param conn      Connection to DB using JDBC, please references ConnectionFactory for mor information
     * @param o         Object that will be that matches data base entry.
     * @throws IllegalAccessException
     * @throws SQLException
     */
    public void sqlDelete(Connection conn, Object o) throws IllegalAccessException, SQLException {
        Class<?> oClass = Objects.requireNonNull(o.getClass());
        
        if (!oClass.isAnnotationPresent(Entity.class)) {
            throw new RuntimeException("Not an Entity type.");
        }
        
        //get name of table which is in entity name or if empty use class name
        Entity anoEntity = oClass.getAnnotation(Entity.class);
        String tableName = anoEntity.name();
        Integer intId = 0;
        String sql = "delete from " + tableName + " where ";
        Field[] oClassFields = oClass.getDeclaredFields();
        for (Field field : oClassFields) {
            field.setAccessible(true);
            
            if (field.isAnnotationPresent(Id.class)) {
                Column cn = field.getAnnotation(Column.class);
                sql += cn.name() + " = (?)";
                intId = (Integer)field.get(o);
            }
        }
        System.out.println(sql);
        
        PreparedStatement pstmt = conn.prepareStatement(sql);
        
        pstmt.setInt(1,intId);
        pstmt.executeUpdate();
        
    }
    
    /**
     * Method to determine if the unique provided columns are not in the database.
     * @param conn
     * @param o
     * @return
     * @throws SQLException
     * @throws IllegalAccessException
     */
    @SuppressWarnings({"unchecked"})
    public Boolean isEntryUnique(Connection conn, Object o) throws SQLException, IllegalAccessException {
        Class<?> oClass = Objects.requireNonNull(o.getClass());
        
        if(!oClass.isAnnotationPresent(Entity.class)){
            throw new RuntimeException("Not an Entity type.");
        }
        //sql statement to be prepared
        Entity anoEntity = oClass.getAnnotation(Entity.class);
        String tableName = anoEntity.name();
        String sql = "select * from "+ tableName+ " where ";
        
        //get the id for look up
        
        String value = "";
        
        Field[] oClassFields = oClass.getDeclaredFields();
        
        for(Field field : oClassFields){
            field.setAccessible(true);
            Column cn = field.getAnnotation(Column.class);
            if(cn.unique() && field.get(o)!=null){
                value = field.get(o).toString();
                sql+=cn.name() +"=\'"+ value+"\'and ";
            }
            
            field.setAccessible(false);
        }
        String newSql = sql.substring(0,sql.length()-4);
        System.out.println(newSql);
        PreparedStatement pstmt = conn.prepareStatement(newSql);
        ResultSet rs = pstmt.executeQuery();
        return !rs.next();
    }
    
}
