package com.pacukievich.lab6.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pacukievich.lab6.model.FieldRequest;
import com.pacukievich.lab6.model.TableRequest;
import com.pacukievich.lab6.service.DumpService;
import com.pacukievich.lab6.service.TableService;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

@RestController
@RequestMapping("/api/admin")
public class ApiAdmController {
		private final TableService tableService;
		private final DumpService dumpService;

		public ApiAdmController(TableService tableService, DumpService dumpService) {
				this.tableService = tableService;
				this.dumpService = dumpService;
		}

		@PostMapping("/create")
		public ResponseEntity<?> createTable(@RequestParam String tableName,
		                                     @RequestParam List<String> fields_name,
		                                     @RequestParam List<String> fields_type,
		                                     @RequestParam(required = false) List<String> fields_notnull,
		                                     @RequestParam(required = false) List<String> fields_unique,
		                                     @RequestParam(required = false) List<String> fields_primary,
		                                     @RequestParam(required = false) List<String> fields_default) {
				try {
						TableRequest tableRequest = new TableRequest();
						tableRequest.setTableName(tableName);

						List<FieldRequest> fields = new ArrayList<>();
						for (int i = 0; i < fields_name.size(); i++) {
								FieldRequest field = new FieldRequest();
								field.setName(fields_name.get(i));
								field.setType(fields_type.get(i));

								field.setNotNull(fields_notnull != null && fields_notnull.contains("true"));
								field.setUnique(fields_unique != null && fields_unique.contains("true"));
								field.setPrimaryKey(fields_primary != null && fields_primary.contains("true"));

								if (fields_default != null && i < fields_default.size() && !fields_default.get(i).isEmpty()) {
										field.setDefaultValue(fields_default.get(i));
								}

								fields.add(field);
						}
						tableRequest.setFields(fields);

						tableService.createTable(tableRequest);
						return ResponseEntity.ok("Таблица создана успешно!");
				} catch (Exception e) {
						return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
										.body("Ошибка при создании таблицы: " + e.getMessage());
				}
		}

		@GetMapping("/tables")
		public ResponseEntity<List<String>> showTables() {
				List<String> tables = tableService.getAllTables();
				tables.removeIf(table -> "saved_queries".equals(table));
				return ResponseEntity.ok(tables);
		}

		@PostMapping("/delete")
		public ResponseEntity<?> deleteTable(@RequestParam String tableName) {
				try {
						tableService.deleteTable(tableName);
						return ResponseEntity.ok("Таблица удалена");
				} catch (Exception e) {
						return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
										.body("Ошибка: " + e.getMessage());
				}
		}

		@GetMapping("/{tableName}")
		public ResponseEntity<?> viewTable(@PathVariable String tableName) {
				try {
						List<Map<String, Object>> tableData = tableService.getTableData(tableName);
						return ResponseEntity.ok(tableData);
				} catch (Exception e) {
						return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
										.body("Ошибка: " + e.getMessage());
				}
		}

		@GetMapping("/search")
		public List<Map<String, Object>> search(
						@RequestParam String tableName,
						@RequestParam String columnName,
						@RequestParam String term
		) {
				return tableService.searchRowsByColumn(tableName, columnName, term);
		}

		// === Бэкап ===
		@GetMapping("/backup")
		public ResponseEntity<?> backupDatabase() {
				try {
						String dumpFilePath = dumpService.createDump();
						Map<String, String> response = new HashMap<>();
						response.put("message", "Дамп успешно создан");
						response.put("file", dumpFilePath);
						return ResponseEntity.ok(response);
				} catch (Exception e) {
						return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
										.body("Ошибка создания дампа: " + e.getMessage());
				}
		}

		@PostMapping("/restore")
		public ResponseEntity<?> restoreDatabase(@RequestParam("dumpFilePath") String dumpFilePath) {
				try {
						dumpService.restoreDump(dumpFilePath);
						return ResponseEntity.ok("База данных успешно восстановлена");
				} catch (Exception e) {
						return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
										.body("Ошибка восстановления: " + e.getMessage());
				}
		}

		@GetMapping("/download")
		public void downloadBackup(@RequestParam("file") String fileName, HttpServletResponse response) throws IOException {
				File file = new File(fileName);
				if (file.exists()) {
						response.setContentType("application/octet-stream");
						response.setHeader("Content-Disposition", "attachment; filename=" + file.getName());
						org.apache.commons.io.FileUtils.copyFile(file, response.getOutputStream());
				} else {
						response.sendError(HttpServletResponse.SC_NOT_FOUND, "Файл не найден");
				}
		}

		@PostMapping("/upload")
		public ResponseEntity<?> uploadBackup(@RequestParam("file") MultipartFile file) {
				try {
						if (file.isEmpty()) {
								return ResponseEntity.badRequest().body("Файл не выбран");
						}

						File targetFile = new File(System.getProperty("user.home") + File.separator + "Downloads" + File.separator + file.getOriginalFilename());
						file.transferTo(targetFile);

						if (!targetFile.getName().endsWith(".sql")) {
								return ResponseEntity.badRequest().body("Неверный формат файла. Ожидается .sql");
						}

						dumpService.restoreDump(targetFile.getAbsolutePath());
						return ResponseEntity.ok("База данных восстановлена из загруженного бекапа");
				} catch (IOException | InterruptedException e) {
						return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Ошибка восстановления: " + e.getMessage());
				}
		}

		@GetMapping("/{tableName}/export")
		public void exportTable(@PathVariable String tableName, HttpServletResponse response) throws IOException {
				String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
				String fileName = String.format("%s_%s.xlsx", tableName, timestamp);

				response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
				String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8.toString());
				response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + encodedFileName);

				List<Map<String, Object>> tableData = tableService.getTableData(tableName);

				try (XSSFWorkbook workbook = new XSSFWorkbook()) {
						Sheet sheet = workbook.createSheet(tableName);

						if (!tableData.isEmpty()) {
								Row headerRow = sheet.createRow(0);
								int headerCellIndex = 0;
								for (String key : tableData.get(0).keySet()) {
										Cell cell = headerRow.createCell(headerCellIndex++);
										cell.setCellValue(key);
								}

								for (int i = 0; i < tableData.size(); i++) {
										Row row = sheet.createRow(i + 1);
										int cellIndex = 0;
										for (Object value : tableData.get(i).values()) {
												Cell cell = row.createCell(cellIndex++);
												cell.setCellValue(value != null ? value.toString() : "");
										}
								}
						}

						workbook.write(response.getOutputStream());
				}
		}

		@GetMapping("/export-all")
		public void exportAllTables(HttpServletResponse response) throws IOException {
				String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
				String fileName = "full_export_" + timestamp + ".xlsx";

				response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
				String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8.toString());
				response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + encodedFileName);

				List<String> allTables = tableService.getAllTableNames();

				try (XSSFWorkbook workbook = new XSSFWorkbook()) {
						for (String tableName : allTables) {
								List<Map<String, Object>> tableData = tableService.getTableData(tableName);
								Sheet sheet = workbook.createSheet(tableName.length() > 31 ? tableName.substring(0, 31) : tableName);

								if (!tableData.isEmpty()) {
										Row headerRow = sheet.createRow(0);
										int headerCellIndex = 0;
										for (String key : tableData.get(0).keySet()) {
												Cell cell = headerRow.createCell(headerCellIndex++);
												cell.setCellValue(key);
										}

										for (int i = 0; i < tableData.size(); i++) {
												Row row = sheet.createRow(i + 1);
												int cellIndex = 0;
												for (Object value : tableData.get(i).values()) {
														Cell cell = row.createCell(cellIndex++);
														cell.setCellValue(value != null ? value.toString() : "");
												}
										}
								}
						}

						workbook.write(response.getOutputStream());
				}
		}

		@PostMapping("/{tableName}/add-columns")
		public ResponseEntity<?> addColumns(@PathVariable String tableName,
		                                    @RequestParam List<String> fields_name,
		                                    @RequestParam List<String> fields_type,
		                                    @RequestParam(required = false) List<String> fields_notnull) {
				try {
						tableService.addColumns(tableName, fields_name, fields_type, fields_notnull);
						return ResponseEntity.ok("Столбцы добавлены");
				} catch (Exception e) {
						return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Ошибка: " + e.getMessage());
				}
		}

		@PostMapping("/{tableName}/delete-column")
		public ResponseEntity<?> deleteColumn(@PathVariable String tableName,
		                                      @RequestParam("columnName") String columnName) {
				try {
						tableService.deleteColumn(tableName, columnName);
						return ResponseEntity.ok("Столбец удален");
				} catch (Exception e) {
						return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Ошибка: " + e.getMessage());
				}
		}

		@PostMapping("/{tableName}/delete-row")
		public ResponseEntity<?> deleteRow(@PathVariable String tableName,
		                                   @RequestParam("id") String id) {
				try {
						String decodedTableName = URLDecoder.decode(tableName, StandardCharsets.UTF_8);
						tableService.deleteRow(decodedTableName, id);
						return ResponseEntity.ok("Удалено успешно");
				} catch (Exception e) {
						return ResponseEntity.badRequest().body("Ошибка: " + e.getMessage());
				}
		}

		@PostMapping("/{tableName}/update")
		public ResponseEntity<?> updateRows(@PathVariable String tableName,
		                                    @RequestParam("updatedJson") String updatedJson) {
				try {
						ObjectMapper mapper = new ObjectMapper();
						List<Map<String, String>> updates = mapper.readValue(updatedJson, new TypeReference<>() {});

						for (Map<String, String> row : updates) {
								if (row.containsKey("id")) {
										String id = row.remove("id");
										tableService.updateOrInsertRow(tableName, id, row);
								}
						}
						return ResponseEntity.ok("Изменения сохранены");
				} catch (Exception e) {
						return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Ошибка: " + e.getMessage());
				}
		}

		@PostMapping("/{tableName}/delete-columns")
		public ResponseEntity<?> deleteColumns(@PathVariable String tableName, @RequestBody List<String> columns) {
				try {
						tableService.deleteColumns(tableName, columns);
						return ResponseEntity.ok("Столбцы удалены");
				} catch (Exception e) {
						return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Ошибка: " + e.getMessage());
				}
		}
}
