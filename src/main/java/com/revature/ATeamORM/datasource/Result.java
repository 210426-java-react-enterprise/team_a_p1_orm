package com.revature.ATeamORM.datasource;

import java.util.List;

/**
 * Maintains a list of objects pulled from the database and provides basic retrieval methods for it.
 * @param <T>
 */
public class Result<T> {

	private final List<T> list;

	public Result(List<T> list) {
		this.list = list;
	}

	/**
	 * Gets entire list of objects pulled from the database
	 * @return The entire list of objects
	 * @author Uros Vorkapic
	 */
	public List<T> getList() {
		return list;
	}

	/**
	 * Gets the first entry in the list of objects pulled from the database. Should be used for queries one expects
	 * to be unique.
	 * @return The first entry of list
	 * @author Uros Vorkapic
	 */
	public T getFirstEntry() {
		if (list == null || list.isEmpty()) {
			return null;
		}
		return list.get(0);
	}
}
