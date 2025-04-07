package com.pacukievich.lab6.service;

import com.pacukievich.lab6.model.FieldRequest;
import com.pacukievich.lab6.model.TableRequest;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class TableService {
		private final JdbcTemplate jdbcTemplate;

		public TableService(JdbcTemplate jdbcTemplate) {
				this.jdbcTemplate = jdbcTemplate;
		}

		public void createTable(TableRequest request) {
				String sql = generateCreateTableSQL(request);
				jdbcTemplate.execute(sql);
		}

		private String generateCreateTableSQL(TableRequest request) {
				StringBuilder sql = new StringBuilder("CREATE TABLE " + request.getTableName() + " (");

				boolean hasPrimaryKey = false;
				List<String> columnDefinitions = new ArrayList<>();
				List<String> primaryKeys = new ArrayList<>(); // Список для хранения всех PRIMARY KEY

				for (FieldRequest field : request.getFields()) {
						StringBuilder columnDef = new StringBuilder(field.getName() + " " + field.getType());

						if (field.isNotNull()) {
								columnDef.append(" NOT NULL");
						}

						if (field.isUnique()) {
								columnDef.append(" UNIQUE");
						}

						if (field.isPrimaryKey()) {
								primaryKeys.add(field.getName()); // Добавляем поле в список PRIMARY KEY
								hasPrimaryKey = true;
						}

						columnDefinitions.add(columnDef.toString());
				}

				// Если ни одно поле не имеет PRIMARY KEY, добавляем id SERIAL PRIMARY KEY
				if (!hasPrimaryKey) {
						columnDefinitions.add(0, "id SERIAL PRIMARY KEY");
				} else {
						sql.append(String.join(", ", columnDefinitions));
						sql.append(", PRIMARY KEY (").append(String.join(", ", primaryKeys)).append(")");
						sql.append(");");
						return sql.toString();
				}

				sql.append(String.join(", ", columnDefinitions)).append(");");

				return sql.toString();
		}
		// Получает список всех таблиц в базе данных
		public List<String> getAllTables() {
				return jdbcTemplate.queryForList("SELECT table_name FROM information_schema.tables WHERE table_schema = 'public'", String.class);
		}

		// Удаляет таблицу из базы данных
		public void deleteTable(String tableName) {
				String sql = "DROP TABLE IF EXISTS \"" + tableName + "\" CASCADE";
				jdbcTemplate.execute(sql);
		}


		public List<Map<String, Object>> getTableData(String tableName) {
				// Заключаем имя таблицы в двойные кавычки для корректной обработки пробелов и спецсимволов
				String query = "SELECT * FROM \"" + tableName + "\"";
				return jdbcTemplate.queryForList(query);
		}


		// Добавление столбцов в таблицу
		public void addColumns(String tableName, List<String> fieldsName, List<String> fieldsType, List<String> fieldsNotNull) {
				for (int i = 0; i < fieldsName.size(); i++) {
						String columnName = fieldsName.get(i);
						String columnType = fieldsType.get(i);
						boolean notNull = fieldsNotNull != null && fieldsNotNull.contains(String.valueOf(i));
						String sql = "ALTER TABLE \"" + tableName + "\" ADD COLUMN \"" + columnName + "\" " + columnType;
						if (notNull) {
								sql += " NOT NULL";
						}
						jdbcTemplate.execute(sql);
				}
		}

		// Удаление столбца из таблицы
		public void deleteColumn(String tableName, String columnName) {
				String sql = "ALTER TABLE \"" + tableName + "\" DROP COLUMN \"" + columnName + "\" CASCADE";
				jdbcTemplate.execute(sql);
		}

		public void deleteRow(String tableName, String id) {
				try {
						Long longId = Long.parseLong(id);
						String sql = "DELETE FROM \"" + tableName + "\" WHERE \"id\" = ?";
						jdbcTemplate.update(sql, longId);
				} catch (NumberFormatException e) {
						throw new IllegalArgumentException("Некорректный формат ID: " + id, e);
				}
		}

		// Обновление строки с автоматическим приведением типов
		public void updateRow(String tableName, String id, Map<String, String> updateData) {
				try {
						Long longId = Long.parseLong(id);
						List<String> setClauses = new ArrayList<>();
						List<Object> params = new ArrayList<>();

						for (Map.Entry<String, String> entry : updateData.entrySet()) {
								String column = entry.getKey();
								// Если значение пустое, заменяем на null
								String value = (entry.getValue() == null || entry.getValue().trim().isEmpty()) ? null : entry.getValue();

								// Получаем SQL-тип колонки автоматически
								String sqlType = getSqlTypeForColumn(tableName, column);
								if (sqlType == null) {
										throw new RuntimeException("Не удалось определить тип для колонки " + column);
								}

								// Формируем выражение с кастом
								setClauses.add("\"" + column + "\" = CAST(? AS " + sqlType + ")");
								params.add(value);
						}
						params.add(longId); // Добавляем id как последний параметр

						String sql = "UPDATE \"" + tableName + "\" SET " + String.join(", ", setClauses) + " WHERE \"id\" = ?";

						System.out.println("📤 SQL для updateRow: " + sql);
						System.out.println("📦 Параметры для updateRow: " + params);

						int rowsUpdated = jdbcTemplate.update(sql, params.toArray());
						if (rowsUpdated > 0) {
								System.out.println("Строка обновлена успешно.");
						} else {
								System.out.println("Обновление не затронуло ни одной строки.");
						}
				} catch (NumberFormatException e) {
						throw new IllegalArgumentException("Некорректный формат ID: " + id, e);
				}
		}

		public void insertRow(String tableName, String id, Map<String, String> rowData) {
				// Добавляем id в данные (так как оно передается пользователем)
				rowData.put("id", id);

				List<String> columns = new ArrayList<>();
				List<String> placeholders = new ArrayList<>();
				List<Object> values = new ArrayList<>();

				for (Map.Entry<String, String> entry : rowData.entrySet()) {
						String column = entry.getKey();
						// Преобразуем пустую строку в null
						String value = (entry.getValue() == null || entry.getValue().trim().isEmpty()) ? null : entry.getValue();
						// Получаем SQL-тип колонки автоматически
						String sqlType = getSqlTypeForColumn(tableName, column);
						if (sqlType == null) {
								throw new RuntimeException("Не удалось определить тип для колонки " + column);
						}
						columns.add("\"" + column + "\"");
						placeholders.add("CAST(? AS " + sqlType + ")");
						// Для поля id, если оно числовое, преобразуем строку в число
						if ("id".equals(column)) {
								values.add(Integer.parseInt(value));
						} else {
								values.add(value);
						}
				}

				String sql = "INSERT INTO \"" + tableName + "\" (" + String.join(", ", columns) + ") VALUES (" + String.join(", ", placeholders) + ")";
				System.out.println("📤 SQL для insertRow: " + sql);
				System.out.println("📦 Параметры для insertRow: " + values);

				jdbcTemplate.update(sql, values.toArray());
		}
		// Универсальный метод для проверки существования строки и обновления/вставки
		public void updateOrInsertRow(String tableName, String id, Map<String, String> updateData) {
				try {
						Long longId = Long.parseLong(id);
						String checkSql = "SELECT COUNT(*) FROM \"" + tableName + "\" WHERE \"id\" = ?";
						int count = jdbcTemplate.queryForObject(checkSql, new Object[]{longId}, Integer.class);

						if (count == 0) {
								System.out.println("Строка с id " + id + " не существует. Выполняем добавление строки.");
								updateData.put("id", id);
								insertRow(tableName, id, updateData);
						} else {
								System.out.println("Строка с id " + id + " существует. Выполняем обновление.");
								if (updateData.containsKey("mouse") && updateData.get("mouse").isEmpty()) {
										updateData.put("mouse", null);
								}
								updateRow(tableName, id, updateData);
						}
				} catch (NumberFormatException e) {
						throw new IllegalArgumentException("Некорректный формат ID: " + id, e);
				}
		}
		/**
		 * Получает SQL-тип колонки через information_schema.
		 * Возвращает тип в виде строки, например "INTEGER", "VARCHAR", "DATE" и т.д.
		 */
		private String getSqlTypeForColumn(String tableName, String columnName) {
				String sql = "SELECT UPPER(data_type) FROM information_schema.columns " +
								"WHERE table_schema = 'public' AND table_name = ? AND column_name = ?";
				try {
						String dataType = jdbcTemplate.queryForObject(sql, new Object[]{tableName, columnName}, String.class);
						if (dataType != null) {
								// Пример преобразования типов:
								if (dataType.contains("CHARACTER VARYING")) {
										return "VARCHAR";
								}
								return dataType;
						}
						return null;
				} catch (Exception e) {
						System.err.println("Ошибка получения типа для колонки " + columnName + " таблицы " + tableName + ": " + e.getMessage());
						return null;
				}
		}
		public void deleteColumns(String tableName, List<String> columns) {
				if (tableName == null || tableName.isBlank()) {
						throw new IllegalArgumentException("Имя таблицы не указано");
				}

				if (columns == null || columns.isEmpty()) {
						throw new IllegalArgumentException("Список удаляемых столбцов пуст");
				}

				// Валидация имён столбцов
				for (String column : columns) {
						if (column == null || column.isBlank() || column.equalsIgnoreCase("id")) {
								throw new IllegalArgumentException("Недопустимое имя столбца для удаления: " + column);
						}
				}

				// Безопасное экранирование имён таблицы и столбцов для PostgreSQL
				String sanitizedTable = tableName.replaceAll("[^\\w\\d_]", "");
				String sql = "ALTER TABLE \"" + sanitizedTable + "\" " +
								columns.stream()
												.map(col -> "DROP COLUMN \"" + col.replaceAll("\"", "") + "\"")
												.collect(Collectors.joining(", "));

				System.out.println("SQL для удаления столбцов: " + sql);

				try {
						jdbcTemplate.execute(sql);
				} catch (DataAccessException e) {
						System.err.println("Ошибка при удалении столбцов: " + e.getMessage());
						throw new RuntimeException("Ошибка при выполнении SQL-запроса", e);
				}
		}
		public List<String> getAllTableNames() {
				return jdbcTemplate.queryForList(
								"SELECT table_name FROM information_schema.tables WHERE table_schema='public' AND table_type='BASE TABLE'",
								String.class
				);
		}

}

