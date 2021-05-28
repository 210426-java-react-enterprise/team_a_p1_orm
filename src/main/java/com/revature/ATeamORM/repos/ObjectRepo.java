package com.revature.ATeamORM.repos;

import com.revature.ATeamORM.exceptions.DataSourceException;
import com.revature.ATeamORM.util.annotations.Column;
import com.revature.ATeamORM.util.annotations.Id;
import com.revature.ATeamORM.util.annotations.Table;
import com.revature.ATeamORM.util.datasource.Result;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import com.revature.ATeamORM.util.annotations.Entity;
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

            // Filters through methods in class for getters
            Method[] getMethods = Arrays.stream(oClass.getDeclaredMethods())
                                        .filter(m -> m.getName().contains("get"))
                                        .toArray(Method[]::new);

            StringBuilder sql = new StringBuilder("insert into " + getTableName(oClass) + " (");
            int i = 1;
            // Gets the column_name and appends it to the sql string for each field annotated with @Column
            for (Field field: fields) {
                field.setAccessible(true);
                field.set(object, 5);
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
                // Used to create a camelCase string for the getter method that will be invoked
                String fieldCapital = field.getName().substring(0, 1).toUpperCase() + field.getName().substring(1);

                // Finds the appropriate getField method and invokes it to get the fieldValue
                for(Method method: getMethods) {
                    if (method.getName().equals("get" + fieldCapital)) {
                        sql.append(method.invoke(object));
                        break;
                    }
                }
                if (i < fields.length) {
                    sql.append(", ");
                }
                field.setAccessible(false);
                i++;
            }
            sql.append(")");

            // Puts sql string into a prepared statement, executes it, retrieves the id, then inserts new id into object
            PreparedStatement pstmt = conn.prepareStatement(String.valueOf(sql), new String[] { "id" });
            if (pstmt.executeUpdate() != 0) {
                ResultSet rs = pstmt.getGeneratedKeys();
                insertId(rs, object);
            }

        } catch (InvocationTargetException e) {
            System.out.println("Cannot invoke method");
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            System.out.println("Cannot access that object");
            e.printStackTrace();
        }

    }

    @SuppressWarnings({"unchecked"})
    public <T> Result<T> read(Connection conn, Class<?> clazz, String fieldName, T fieldValue) throws SQLException {

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

            Constructor<?> objectConstructor = clazz.getConstructor();
            while(rs.next()) {
                T object = (T) Objects.requireNonNull(objectConstructor.newInstance());
                objectList.add(generateObject(rs, fields, object));
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

            // Filters through methods in class for getters
            Method[] getMethods = Arrays.stream(oClass.getDeclaredMethods())
                                        .filter(m -> m.getName().contains("get"))
                                        .toArray(Method[]::new);

            // Used to create a camelCase string for the getter method that will be invoked
            String fieldCapital = "";
            StringBuilder sql = new StringBuilder("update "  + getTableName(oClass) + " set ");
            int i = 1;

            // Goes through each @Column annotated field in class and appends sql string with "column_name = fieldValue,"
            for (Field field: fields) {
                field.setAccessible(true);
                fieldCapital = field.getName().substring(0, 1).toUpperCase() + field.getName().substring(1);

                // Finds the appropriate getField method and invokes it to get the fieldValue
                for (Method method: getMethods) {
                    if (method.getName().equals("get" + fieldCapital)) {
                        sql.append(getColumnName(field))
                           .append(" = ")
                           .append(method.invoke(object));
                        break;
                    }
                }
                if (i < fields.length) {
                    sql.append(", ");
                }
                field.setAccessible(false);
                i++;
            }

            String idName = "";

            // Finds the field annotated with @Id and gets its field name to build a query of "where id_name = id"
            for (Field field : fields) {
                field.setAccessible(true);
                if (field.isAnnotationPresent(Id.class)) {
                    idName = getColumnName(field);
                    fieldCapital = field.getName().substring(0, 1).toUpperCase() + field.getName().substring(1);
                    break;
                }
                field.setAccessible(false);
            }
            sql.append(" where ")
               .append(idName)
               .append(" = ");

            // Finds method that matches the getField method used to retrieve the id, then appends it to sql string
            for (Method method: getMethods) {
                if (method.getName().equals("get" + fieldCapital)) {
                    sql.append(method.invoke(object));
                }
            }

            // Loads sql string into a PreparedStatement and executes it, updating the database
            // Final sql string will look something like this:
            // update tableName set user_id = object.id, username = object.username, password = object.password where user_id = object.id
            PreparedStatement pstmt = conn.prepareStatement(String.valueOf(sql));
            pstmt.executeUpdate();

        } catch (InvocationTargetException e) {
            System.out.println("Cannot invoke that method");
            e.printStackTrace();
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
            String fieldCapital = field.getName().substring(0, 1).toUpperCase() + field.getName().substring(1);
            field.setAccessible(false);
            Method method = oClass.getMethod("get" + fieldCapital);

            String sql = "delete from " + getTableName(oClass) + " where id = " + method.invoke(object);
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.executeUpdate();

        } catch (NoSuchMethodException e) {
            System.out.println("No such method exists");
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            System.out.println("Cannot invoke that method");
            e.printStackTrace();
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

    private void insertId(ResultSet rs, Object o) throws SQLException, InvocationTargetException, IllegalAccessException {
        Class<?> oClass = Objects.requireNonNull(o.getClass());
        Method[] setMethods = Arrays.stream(oClass.getDeclaredMethods())
                                    .filter(m -> m.getName().contains("set"))
                                    .toArray(Method[]::new);

        while (rs.next()) {
            Field[] fields = oClass.getDeclaredFields();
            String setId = "";
            for (Field field : fields) {
                field.setAccessible(true);
                if (field.isAnnotationPresent(Id.class)) {
                    setId = "set" + field.getName().substring(0, 1).toUpperCase() + field.getName().substring(1);
                    field.setAccessible(false);
                    break;
                }
            }
            for (Method method : setMethods) {
                if (method.getName().equals(setId)) {
                    method.invoke(o, rs.getInt("id"));
                }
            }

        }
    }

    private <T> T generateObject(ResultSet rs, Field[] fields, T object) throws SQLException, IllegalAccessException {
        for (Field f: fields) {
            switch (f.getType().getSimpleName()) {
                case ("String"):
                    f.set(object, rs.getString(getColumnName(f)));
                    break;
                case ("int"):
                case ("Integer"):
                    f.set(object, rs.getInt(getColumnName(f)));
                    break;
                case ("double"):
                case ("Double"):
                    f.set(object, rs.getDouble(getColumnName(f)));
                    break;
                case ("float"):
                case ("Float"):
                    f.set(object, rs.getFloat(getColumnName(f)));
                case ("boolean"):
                    f.set(object, rs.getBoolean(getColumnName(f)));
            }
        }
        return object;
    }

}



