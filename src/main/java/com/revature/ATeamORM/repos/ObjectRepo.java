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

}



