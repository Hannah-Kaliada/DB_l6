package com.pacukievich.lab6.service;

import com.pacukievich.lab6.model.FieldRequest;
import com.pacukievich.lab6.model.TableRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
				String sql = "DROP TABLE IF EXISTS " + tableName + " CASCADE";
				jdbcTemplate.execute(sql);
		}

		public List<Map<String, Object>> getTableData(String tableName) {
				// Заключаем имя таблицы в двойные кавычки для корректной обработки пробелов и спецсимволов
				String query = "SELECT * FROM \"" + tableName + "\"";
				return jdbcTemplate.queryForList(query);
		}


}
