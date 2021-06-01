package com.revature.ATeamORM.repos;

import com.revature.ATeamORM.annotations.Column;
import com.revature.ATeamORM.exceptions.DataSourceException;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Objects;

/**
 * More efficiently creates objects and declares their fields than prior ObjectRepo method.
 * @param <T> Class Type for object being created
 * @author Uros Vorkapic
 */
public class ObjectCreator<T> {

	Class<T> clazz;
	ResultSet rs;
	Field [] fields;
	Constructor<T> constructor;

	/**
	 * Instantiates
	 * @param clazz Blueprint for objects to be created
	 * @param rs ResultSet containing data used to fill class with
	 * @throws NoSuchMethodException Thrown if passed class lacks a no-args constructor.
	 * @author Uros Vorkapic
	 */
	ObjectCreator (Class<T> clazz, ResultSet rs) throws NoSuchMethodException {
		this.clazz = clazz;
		this.rs = rs;
		fields = Arrays.stream(clazz.getDeclaredFields())
					   .filter(f -> f.isAnnotationPresent(Column.class))
					   .toArray(Field[]::new);
		constructor = clazz.getConstructor();
	}

	/**
	 * Creates a new object from class blueprint, populates its fields, and returns it
	 * @return Returns new fully instantiated and declared object of constructed class
	 * @throws InvocationTargetException Handled in ObjectRepo
	 * @throws InstantiationException Handled in ObjectRepo
	 * @throws IllegalAccessException Handled in ObjectRepo
	 * @throws SQLException Handled in ObjectRepo
	 * @author Uros Vorkapic
	 */
	T create() throws InvocationTargetException, InstantiationException, IllegalAccessException, SQLException {
		T object =  Objects.requireNonNull(constructor.newInstance());
		boolean isValid = true;
		for (Field field : fields) {
			field.setAccessible(true);
			switch (field.getType().getSimpleName()) {
				case ("String"):
					field.set(object, rs.getString(getColumnName(field)));
					break;
				case ("int"):
				case ("Integer"):
					field.set(object, rs.getInt(getColumnName(field)));
					break;
				case ("double"):
				case ("Double"):
					field.set(object, rs.getDouble(getColumnName(field)));
					break;
				case ("float"):
				case ("Float"):
					field.set(object, rs.getFloat(getColumnName(field)));
					break;
				case ("boolean"):
				case ("Boolean"):
					field.set(object, rs.getBoolean(getColumnName(field)));
					break;
				default:
					isValid = false;

			}
			field.setAccessible(false);
			if (!isValid) {
				throw new DataSourceException("Invalid field type. Make sure all fields in object are either: \n" +
						"String\n" +
						"int/Integer\n" +
						"double/Double\n" +
						"float/Float\n" +
						"boolean/Boolean");
			}
		}
		return object;
	}

	/**
	 * Simple method to retrieve @Column name(), or the field name if none is provided.
	 * @param field The field with the expected @Column annotation
	 * @return @Column name() or name of field if none provided
	 * @author Uros Vorkapic
	 */
	private String getColumnName(Field field) {
		String columnName = field.getAnnotation(Column.class).name();
		if (columnName.equals("")) {
			columnName = field.getName();
		}
		return columnName;
	}

}
