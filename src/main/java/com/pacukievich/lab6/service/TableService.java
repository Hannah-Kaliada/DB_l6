package com.pacukievich.lab6.service;

import com.pacukievich.lab6.model.FieldRequest;
import com.pacukievich.lab6.model.TableRequest;
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

		public void updateOrInsertRow(String tableName, String id, Map<String, String> updateData) {
				try {
						Long longId = Long.parseLong(id);

						// Проверка существования строки с таким id
						String checkSql = "SELECT COUNT(*) FROM \"" + tableName + "\" WHERE \"id\" = ?";
						int count = jdbcTemplate.queryForObject(checkSql, new Object[]{longId}, Integer.class);

						if (count == 0) {
								// Если строки нет, выполняем INSERT
								System.out.println("Строка с id " + id + " не существует. Выполняем добавление строки.");
								updateData.put("id", id); // Добавляем id в данные для вставки
								insertRow(tableName, id, updateData); // Вставляем строку
						} else {
								// Если строка существует, выполняем UPDATE
								System.out.println("Строка с id " + id + " существует. Выполняем обновление.");
								// Обработка пустых значений для "mouse" (если нужно)
								if (updateData.containsKey("mouse") && updateData.get("mouse").isEmpty()) {
										updateData.put("mouse", null); // Заменяем пустое значение на NULL
								}
								// Обновляем строку
								updateRow(tableName, id, updateData);
						}

				} catch (NumberFormatException e) {
						throw new IllegalArgumentException("Некорректный формат ID: " + id, e);
				}
		}

		public void updateRow(String tableName, String id, Map<String, String> updateData) {
				try {
						Long longId = Long.parseLong(id);

						// Формирование части SET запроса
						String setClause = updateData.entrySet().stream()
										.map(entry -> "\"" + entry.getKey() + "\" = ?")
										.collect(Collectors.joining(", "));

						Object[] params = new Object[updateData.size() + 1];
						int index = 0;
						for (Map.Entry<String, String> entry : updateData.entrySet()) {
								params[index++] = entry.getValue();
						}
						params[index] = longId;

						String sql = "UPDATE \"" + tableName + "\" SET " + setClause + " WHERE \"id\" = ?";

						// 🔍 Выводим SQL и параметры
						System.out.println("📤 SQL для updateRow: " + sql);
						System.out.println("📦 Параметры для updateRow: " + Arrays.toString(params));

						// Выполняем запрос на обновление
						jdbcTemplate.update(sql, params);

				} catch (NumberFormatException e) {
						throw new IllegalArgumentException("Некорректный формат ID: " + id, e);
				}
		}

		public void insertRow(String tableName, String id, Map<String, String> rowData) {
				// Преобразуем id в Integer (или Long в зависимости от типа в базе данных)
				Integer intId = Integer.parseInt(id); // Если id - BIGINT, используйте Long.parseLong(id)

				// Добавляем id в rowData для вставки
				rowData.put("id", String.valueOf(intId)); // Преобразуем intId обратно в строку для использования в запросе

				// Формируем список столбцов для вставки
				String columns = rowData.keySet().stream()
								.map(col -> "\"" + col + "\"")
								.collect(Collectors.joining(", "));

				// Формируем плейсхолдеры для параметров
				String placeholders = rowData.keySet().stream()
								.map(col -> "?")
								.collect(Collectors.joining(", "));

				// Преобразуем значения в правильные типы для каждого столбца
				List<Object> values = new ArrayList<>();
				for (String column : rowData.keySet()) {
						String value = rowData.get(column);

						// Преобразуем строковые значения в нужные типы
						if ("id".equals(column)) {
								values.add(intId); // Преобразуем id в Integer
						} else {
								values.add(value); // Остальные значения оставляем как есть
						}
				}

				// Формируем SQL запрос
				String sql = "INSERT INTO \"" + tableName + "\" (" + columns + ") VALUES (" + placeholders + ")";

				// 🔍 Выводим SQL и параметры для отладки
				System.out.println("📤 SQL для insertRow: " + sql);
				System.out.println("📦 Параметры для insertRow: " + values);

				// Выполняем запрос на вставку
				jdbcTemplate.update(sql, values.toArray());
		}





}
