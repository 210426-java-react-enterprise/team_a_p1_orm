package com.revature.ATeamORM.datasource;

import java.util.List;

public class Result<T> {

	private List<T> list;

	public Result(List<T> list) {
		this.list = list;
	}

	public List<T> getList() {
		return list;
	}

	public T getFirstEntry() {
		if (list.isEmpty()) {
			return null;
		}
		return list.get(0);
	}
}
