package com.revature.ATeamORM.repos;

import com.revature.ATeamORM.annotations.Column;
import com.revature.ATeamORM.annotations.Id;
import com.revature.ATeamORM.annotations.Table;
import com.revature.ATeamORM.annotations.Entity;

import com.revature.ATeamORM.datasource.Result;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import java.sql.*;

import com.revature.ATeamORM.exceptions.NullFieldException;


import java.util.*;

/**
 * Provides CRUD+ operations for objects and classes passed in through Session class. Should not be directly invoked
 * outside of the ORM.
 * @author Juan Mendoza, Uros Vorkapic, Vinson Chin
 */
public class ObjectRepo {

    /**
     * Generates a new entry in the database containing information provided in the object. Id is assumed to be serial.
     * @param conn Database connection this operation will be performed in.
     * @param object Entry to be added to database
     * @throws SQLException Thrown if connection cannot be established, object is missing non-null field values or
     * if uniqueness is not ensured
     */
    public void create(Connection conn, Object object) throws SQLException {
        
        try {
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
            for (Field field : fields) {
                field.setAccessible(true);
                if (field.getAnnotation(Column.class).notNull() && field.get(object) == null) {
                    throw new NullFieldException();
                }
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
            for (Field field : fields) {
                field.setAccessible(true);
                sql.append(encapsulateString(field.get(object)));
                if (i < fields.length) {
                    sql.append(", ");
                }
                field.setAccessible(false);
                i++;
            }
            sql.append(")");
            
            Field field = Arrays.stream(oClass.getDeclaredFields())
                                .filter(f -> f.isAnnotationPresent(Id.class))
                                .findFirst()
                                .get();
            field.setAccessible(true);
            String fieldId = getColumnName(field);
            field.setAccessible(false);
            // Puts sql string into a prepared statement, executes it, retrieves the id, then inserts new id back into object
            PreparedStatement pstmt = conn.prepareStatement(sql.toString(), new String[]{fieldId});
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

    /**
     * Finds and returns a Result List of objects instantiated from the clazz Class
     * whose fieldName matches the fieldValue provided
     * @param conn Database connection this operation will be performed in.
     * @param clazz The class reference for the objects to be built from
     * @param fieldName The name of the field (not column) that will be searched
     * @param fieldValue The value of the field as a String
     * @param <T> The object type created from the injected class
     * @return Result object containing a list of all entries returned from query
     * @throws SQLException Thrown if connection cannot be established, fieldName does not exist or
     * if @Column is not properly annotated
     * @author Juan Mendoza, Uros Vorkapic, Vinson Chin
     */
    public <T> Result<T> read(Connection conn, Class<T> clazz, String fieldName, String fieldValue) throws SQLException {
        
        List<T> objectList = new ArrayList<>();
        StringBuilder sql = new StringBuilder("select * from " + getTableName(clazz) + " where ");
        
        // All classes passed in must be annotated with @Entity
        if (!clazz.isAnnotationPresent(Entity.class)) {
            throw new RuntimeException("This is not an entity class!");
        }

        try {
            Field field = clazz.getDeclaredField(fieldName);
            sql.append(getColumnName(field))
               .append(" = ")
               .append(encapsulateString(fieldValue));


            PreparedStatement pstmt = conn.prepareStatement(sql.toString());
            ResultSet rs = pstmt.executeQuery();

            ObjectCreator<T> oCreator = new ObjectCreator<>(clazz, rs);
            while(rs.next()) {
                objectList.add(oCreator.create());
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

    /**
     * Overload of read function. Retrieves all entries from table in database as a Result list of objects
     * @param conn Database connection this operation will be performed in.
     * @param clazz The class reference for the objects to be built from
     * @param <T> Cass Type
     * @return Result list of objects from database
     * @throws SQLException Thrown if connection cannot be established or if class @Columns are not properly annotated
     * @author Uros Vorkapic
     */
    public <T> Result<T> read(Connection conn, Class<T> clazz) throws SQLException {
        List<T> objectList = new ArrayList<>();
        PreparedStatement pstmt = conn.prepareStatement("select * from " + getTableName(clazz));

        ResultSet rs = pstmt.executeQuery();
        try {
            ObjectCreator<T> oCreator = new ObjectCreator<>(clazz, rs);
            while (rs.next()) {
                objectList.add(oCreator.create());
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
        }
        return new Result<>(objectList);
    }

    /**
     * Saves/Updates the values of the object provided into the database based on the @Id annotated field of the object
     * @param conn Database connection this operation will be performed in.
     * @param object The object with non-null fields to use to update the database with
     * @throws SQLException Thrown if connection cannot be established, object is missing field values or if
     * ID cannot be found.
     * @author Juan Mendoza, Uros Vorkapic, Vinson Chin
     */
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
            StringBuilder sql = new StringBuilder("update " + getTableName(oClass) + " set ");
            int i = 1;
            
            // Goes through each @Column annotated field in class and appends sql string with "column_name = fieldValue,"
            for (Field field : fields) {
                field.setAccessible(true);
                if (field.getAnnotation(Column.class).notNull() && field.get(object) == null) {
                    throw new NullFieldException();
                }
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

    /**
     * Deletes the provided object from the database entirely using @Id annotated object field
     * @param conn Database connection this operation will be performed in.
     * @param object The object to be removed from the database
     * @throws SQLException Thrown if connection cannot be established or something went terribly wrong
     * @author Juan Mendoza, Uros Vorkapic, Vinson Chin
     */
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
                                .findFirst()
                                .get();
            
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

    /**
     * Checks database to see if columns with unique() = true already exist. True means they do not, false means they do.
     * @param conn Database connection this operation will be performed in.
     * @param o Object containing information to be checked if unique
     * @return true if entry does not exist in database, false otherwise. Always returns false if no @Columns are declared unique()
     * @throws SQLException Thrown if connection cannot be established or @Columns are not properly annotated
     * @author Juan Mendoza, Uros Vorkapic
     */
    public Boolean isEntryUnique(Connection conn, Object o) throws SQLException {
        Class<?> oClass = Objects.requireNonNull(o.getClass());

        if(!oClass.isAnnotationPresent(Entity.class)){
            throw new RuntimeException("Not an Entity type.");
        }

        Field[] oClassFields = Arrays.stream(oClass.getDeclaredFields())
                                     .filter(f -> f.isAnnotationPresent(Column.class) && f.getAnnotation(Column.class).unique())
                                     .toArray(Field[]::new);
        // if no @Columns are unique(), returns false by default so database doesn't have to be needlessly opened
        if (oClassFields.length == 0) {
            return false;
        }

        StringBuilder sql = new StringBuilder("select * from " + getTableName(oClass) + " where ");
        try {
            for(Field field : oClassFields){
                field.setAccessible(true);
                Column cn = field.getAnnotation(Column.class);
                if(cn.unique() && field.get(o) != null){
                    sql.append(cn.name())
                       .append(" = ")
                       .append(encapsulateString(field.get(o)))
                       .append(" and ");
                }
                field.setAccessible(false);
            }
        } catch (IllegalAccessException e) {
            System.out.println("Could not access field!");
            e.printStackTrace();
        }

        String newSql = sql.substring(0,sql.length()-4);
        PreparedStatement pstmt = conn.prepareStatement(newSql);
        ResultSet rs = pstmt.executeQuery();
        return !rs.next();
    }

    /**
     * Simple method to retrieve @Table name(), or the class name if none is provided.
     * @param clazz The class with the expected @Table annotation
     * @return @Table name() or name of class if none provided
     * @author Uros Vorkapic
     */
    private String getTableName(Class<?> clazz) {
        String tableName = clazz.getAnnotation(Table.class)
                                .name();
        if (tableName.equals("")) {
            tableName = clazz.getName();
        }
        return tableName;
    }

    /**
     * Simple method to retrieve @Column name(), or the field name if none is provided.
     * @param field The field with the expected @Column annotation
     * @return @Column name() or name of field if none provided
     * @author Uros Vorkapic
     */
    private String getColumnName(Field field) {
        String columnName = field.getAnnotation(Column.class)
                                 .name();
        if (columnName.equals("")) {
            columnName = field.getName();
        }
        return columnName;
    }


    /**
     * Ensures strings are properly encapsulated in single quotes before fed into sql query
     * @param t Data to be encapsulated or not
     * @param <T> Data Type
     * @return String containing either a String encapsulated in single quotes, or a String of the data provided
     * @author Uros Vorkapic
     */
    private <T> String encapsulateString (T t) {
        StringBuilder s = new StringBuilder();
        if (t.getClass()
             .getSimpleName()
             .equals("String")) {
            s.append("\'")
             .append(t)
             .append("\'");
        } else {
            s.append(t);
        }
        return s.toString();
    }

    /**
     * Mostly defunct. Only used in one instance. A singular version of ObjectCreator.create(). Makes code a little more
     * efficient by not having to instantiate the object to perform a single operation.
     * @param rs The ResultSet containing generated key
     * @param field The Field being updated
     * @param object The Object whose field will be updated
     * @param dbID The id name in the database (value retrieved using rs)
     * @param <T> Object type
     * @throws SQLException Thrown because of ResultSet
     * @throws IllegalAccessException Thrown if field does not exist in object.
     * @author Uros Vorkapic
     */
    private <T> void setObjectValues(ResultSet rs, Field field, T object, String dbID) throws SQLException, IllegalAccessException {
        field.setAccessible(true);
        switch (field.getType()
                     .getSimpleName()) {
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
    
}


