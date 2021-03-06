package se.emilsjolander.sprinkles;

import java.util.ArrayList;
import java.util.List;

import se.emilsjolander.sprinkles.exceptions.NoSuchColumnFoundException;
import android.database.sqlite.SQLiteDatabase;

public class Migration {

	private List<String> mStatements = new ArrayList<String>();

	void execute(SQLiteDatabase db) {
		for (String sql : mStatements) {
			db.execSQL(sql);
		}
	}
	
	public void createTable(Class<? extends Model> clazz) {
		final String tableName = Utils.getTableName(clazz);
		final StringBuilder createStatement = new StringBuilder();

		createStatement.append("CREATE TABLE ");
		createStatement.append(tableName);
		createStatement.append("(");

		final List<ColumnField> columns = Utils.getColumns(clazz);
		final List<ColumnField> primaryColumns = new ArrayList<ColumnField>();
		final List<ColumnField> foreignColumns = new ArrayList<ColumnField>();
		for (int i = 0; i < columns.size(); i++) {
			final ColumnField column = columns.get(i);
			createStatement.append(column.name + " ");
			createStatement.append(column.type);

			if (column.isAutoIncrementPrimaryKey) {
				createStatement.append(" PRIMARY KEY AUTOINCREMENT");
			}else {
				if (column.isPrimaryKey) {
					primaryColumns.add(column);
				}

				if (column.isForeignKey) {
					foreignColumns.add(column);
				}
			}

			// add a comma separator between columns if it is not the last
			// column
			if (i < columns.size() - 1 || !primaryColumns.isEmpty()
					|| !foreignColumns.isEmpty()) {
				createStatement.append(", ");
			}
		}

		if (!primaryColumns.isEmpty()) {
			createStatement.append("PRIMARY KEY(");

			for (int i = 0; i < primaryColumns.size(); i++) {
				final ColumnField column = primaryColumns.get(i);
				createStatement.append(column.name);

				// add a comma separator between keys if it is not the last
				// primary key
				if (i < primaryColumns.size() - 1) {
					createStatement.append(", ");
				}
			}

			createStatement.append(")");

			// add a comma separator if there are foreign keys to add
			if (!foreignColumns.isEmpty()) {
				createStatement.append(", ");
			}
		}

		for (int i = 0; i < foreignColumns.size(); i++) {
			final ColumnField column = foreignColumns.get(i);
			createStatement.append("FOREIGN KEY(");
			createStatement.append(column.name);
			createStatement.append(") REFERENCES ");
			createStatement.append(column.foreignKey);
			if (column.isCascadeDelete) {
				createStatement.append(" ON DELETE CASCADE");
			}

			// add a comma separator if there are still foreign keys to add
			if (i < foreignColumns.size() - 1) {
				createStatement.append(", ");
			}
		}

		createStatement.append(");");

		mStatements.add(createStatement.toString());
	}

	public void dropTable(Class<? extends Model> clazz) {
		final String tableName = Utils.getTableName(clazz);
		mStatements.add(String.format("DROP TABLE IF EXISTS %s;", tableName));
	}

	public void renameTable(String from, String to) {
		mStatements.add(String.format("ALTER TABLE %s RENAME TO %s;", from, to));
	}

	public void addColumn(Class<? extends Model> clazz, String columnName) {
		final String tableName = Utils.getTableName(clazz);
		ColumnField column = null;

		List<ColumnField> fields = Utils.getColumns(clazz);
		for (ColumnField field : fields) {
			if (field.name.equals(columnName)) {
				column = field;
			}
		}

		if (column == null) {
			throw new NoSuchColumnFoundException(columnName);
		}

		mStatements.add(String.format("ALTER TABLE %s ADD COLUMN %s %s;",
				tableName, column, column.type));
	}
	
	public void addRawStatement(String statement) {
		mStatements.add(statement);
	}

}
