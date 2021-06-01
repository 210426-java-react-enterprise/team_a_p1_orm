package com.revature.ATeamORM.datasource;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class ResultTest {

	private List<String> list;
	private Result<String> sut;

	@Before
	public void setUpTest() {
		list = new ArrayList<>();
		sut = new Result<>(list);
	}

	@After
	public void tearDownTest() {
		sut = null;
		list = null;
	}

	@Test
	public void test_getListWithValidList() {
		list.add("a");
		list.add("b");
		List<String> expectedList = list;

		List<String> actualList = sut.getList();
		Assert.assertEquals(expectedList, actualList);
	}

	@Test
	public void test_getListWithInvalidList() {
		list = null;
		sut = new Result<>(list);
		Assert.assertNull(sut.getList());
	}

	@Test
	public void test_getFirstEntryWithValidList() {
		list.add("a");
		String expectedString = "a";

		String actualString = sut.getFirstEntry();
		Assert.assertEquals(expectedString, actualString);
	}

	@Test
	public void test_getFirstEntryWithNullList() {
		list = null;
		sut = new Result<>(list);
		Assert.assertNull(sut.getFirstEntry());
	}

	@Test
	public void test_getFirstEntryWithEmptyList() {
		Assert.assertNull(sut.getFirstEntry());
	}


}
