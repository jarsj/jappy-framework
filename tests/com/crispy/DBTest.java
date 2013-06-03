package com.crispy;

import java.sql.SQLException;
import java.util.List;

import junit.framework.Assert;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.crispy.Index.IndexType;
import com.crispy.Table.EngineType;
import com.crispy.Table.MatchMode;

public class DBTest {

	@BeforeClass
	public static void setUp() throws Exception {
		DB.init("test", "root", "harsh");
		Table.get("test").drop(true);
	}

	@Test
	public void checkTableMetadata() throws SQLException {
		DB.updateQuery("CREATE TABLE `test` (`name` VARCHAR(100))");
		DB.updateQuery("ALTER TABLE `test` ADD INDEX (`name`)");

		DB.loadMetadata("test");
		Metadata m = DB.getMetadata("test");
		Assert.assertSame(m.indexes.size(), 1);
		Assert.assertEquals(m.indexes.get(0).name, "name");

		DB.updateQuery("DROP TABLE `test`");
	}

	@Test
	public void checkTableCration() throws Exception {
		try {
			Table.get("movie").drop(true);
			Table.get("celebrity").drop(true);
		} catch (Throwable t) {
		}
		Table.get("celebrity")
				.columns(Column.bigInteger("id", true),
						Column.text("name", 100)).create();

		Table.get("movie").columns(//
				Column.bigInteger("id", true), //
				Column.text("name", 200), //
				Column.text("description", 200),
				Column.bigInteger("director")).indexes(Index.create("name"))
				.constraints(Constraint.create("director", "celebrity", "id"))
				.display("name").create();
		Metadata m = DB.getMetadata("movie");
		Assert.assertNotNull(m.primary);
		Assert.assertSame(m.indexes.size(), 1);
		Assert.assertSame(m.constraints.size(), 1);
		Assert.assertEquals(m.getDisplay(), "name");

		Table.get("celebrity").columns("name").values("Harsh").add();
		long id = Table.get("celebrity").columns("id").row().biginteger("id");
		Table.get("movie").columns("name", "description", "director").values("Love You", "What is good", id)
				.add();

		Row r = Table.get("movie").columns("name", "description")
				.join(Table.get("celebrity").columns("name")).row();
		
		try {
			r.string("name");
			Assert.fail("Should throw exception");
		} catch (IllegalArgumentException e) {
			
		}
		
		Assert.assertEquals(r.string("movie", "name"), "Love You");
		Assert.assertEquals(r.string("description"), "What is good");
		Assert.assertEquals(r.string("celebrity", "name"), "Harsh");

		System.out.println(DB.getMetadata("movie"));
		Table.get("movie").columns(//
				Column.text("id", 100), //
				Column.text("name", 200), //
				Column.bigInteger("director")).indexes(Index.create("name"))
				.create();
		m = DB.getMetadata("movie");

		Assert.assertNull(m.primary);
		Assert.assertSame(m.indexes.size(), 1);
		System.out.println(DB.getMetadata("movie"));
		Table.get("movie").drop(false);

	}

	@Test
	public void testSomething() {
		try {
			Table.get("movie").drop(true);
		} catch (Throwable t) {
		}
		Table.get("movie").columns(Column.text("name", 200), //
				Column.integer("rating"),//
				Column.date("release_date"),//
				Column.integer("runtime"),//
				Column.text("description", 512),//
				Column.bigInteger("collection"), //
				Column.bigInteger("id", true)).create();

		Metadata m = DB.getMetadata("movie");
		for (Column c : m.columns) {
			System.out.println(c.simpleType(m).toString());
		}
	}

	@Test
	public void testMatchAgainst() throws SQLException {
		DB.loadMetadata("search");

		Table.get("search")
				.columns(Column.bigInteger("entity_id"),
						Column.text("entity_type", 50),
						Column.text("title", 200), Column.text("content", 2048))
				.primary("entity_id", "entity_type")
				.indexes(
						Index.create("FULLTEXT", IndexType.FULLTEXT, "title",
								"content")).engine(EngineType.MY_ISAM).create();

		List<Row> rows = Table
				.get("search")
				.columns("entity_type", "title", "content")
				.search(new String[] { "title", "content" }, "Tees Maar Khan",
						MatchMode.IN_NATURAL_LANGUAGE_MODE).rows();
		int count = 0;
		for (Row r : rows) {
			System.out.println(r.column("title") + "--------"
					+ r.column("content"));
			count++;
		}
		System.out.println("TOTAL--------" + count);
	}

	@AfterClass
	public static void tearDown() throws Exception {
		DB.shutdown();
	}

}
