package com.revature.ATeamORM.repos;


import com.revature.ATeamORM.util.annotations.Column;

import com.revature.ATeamORM.util.annotations.Entity;


import com.revature.ATeamORM.util.annotations.Id;
import com.revature.ATeamORM.util.annotations.Table;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.*;
import java.util.*;

public class ObjectRepo {
    
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
        String idName = "";
        String value = "";
        Integer idInteger = 0;
        Field[] oClassFields = oClass.getDeclaredFields();
        
        for(Field field : oClassFields){
            field.setAccessible(true);
            Column cn = field.getAnnotation(Column.class);
            if(cn.unique()){
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
    @SuppressWarnings({"unchecked"})
    public ResultSet sqlSelect(Connection conn, Object o) throws SQLException, IllegalAccessException {
        Class<?> oClass = Objects.requireNonNull(o.getClass());
        
        if(!oClass.isAnnotationPresent(Entity.class)){
            throw new RuntimeException("Not an Entity type.");
        }
        //sql statement to be prepared
        Entity anoEntity = oClass.getAnnotation(Entity.class);
        String tableName = anoEntity.name();
        String sql = "select * from "+ tableName+ " where ";
        
        //get the id for look up
        String idName = "";
        Integer idInteger = 0;
        Field[] oClassFields = oClass.getDeclaredFields();
        
        for(Field field : oClassFields){
            field.setAccessible(true);
            
            if(field.isAnnotationPresent(Id.class)){
                Column cn = field.getAnnotation(Column.class);
                sql += cn.name()+ "=(?)";
                idInteger = (Integer) field.get(o);
            }
            field.setAccessible(false);
        }
        
        System.out.println(sql);
        PreparedStatement pstmt = conn.prepareStatement(sql);
        pstmt.setInt(1,idInteger);
        return pstmt.executeQuery();
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



