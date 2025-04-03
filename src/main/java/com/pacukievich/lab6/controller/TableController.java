package com.pacukievich.lab6.controller;

import com.pacukievich.lab6.model.TableRequest;
import com.pacukievich.lab6.model.FieldRequest;
import com.pacukievich.lab6.service.DumpService;
import com.pacukievich.lab6.service.TableService;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.ui.Model;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/tables")
public class TableController {
		private final TableService tableService;
		private final DumpService dumpService;

		public TableController(TableService tableService, DumpService dumpService) {
				this.tableService = tableService;
				this.dumpService = dumpService;
		}

		@GetMapping("/new")
		public String showCreateTableForm(Model model) {
				model.addAttribute("table", new TableRequest());
				return "create_table";
		}

		@PostMapping("/create")
		public String createTable(@RequestParam String tableName,
		                          @RequestParam List<String> fields_name,
		                          @RequestParam List<String> fields_type,
		                          @RequestParam(required = false) List<String> fields_notnull,
		                          @RequestParam(required = false) List<String> fields_unique,
		                          @RequestParam(required = false) List<String> fields_primary,
		                          @RequestParam(required = false) List<String> fields_default,
		                          RedirectAttributes redirectAttributes) {
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
						redirectAttributes.addFlashAttribute("success", "Таблица создана успешно!");
				} catch (Exception e) {
						redirectAttributes.addFlashAttribute("error", "Ошибка при создании таблицы: " + e.getMessage());
				}
				return "redirect:/tables";
		}

		@GetMapping
		public String showTables(Model model) {
				List<String> tables = tableService.getAllTables();
				model.addAttribute("tables", tables);
				return "tables";
		}

		@PostMapping("/delete")
		public String deleteTable(@RequestParam String tableName) {
				tableService.deleteTable(tableName);
				return "redirect:/tables";
		}

		@GetMapping("/{tableName}")
		public String viewTable(@PathVariable String tableName, Model model) {
				List<Map<String, Object>> tableData = tableService.getTableData(tableName);
				model.addAttribute("tableName", tableName);
				model.addAttribute("tableData", tableData);
				return "view_table";
		}

		// Создание бекапа
		@GetMapping("/backup")
		public String backupDatabase(Model model) {
				try {
						// Получаем путь к файлу дампа в формате plain (sql)
						String dumpFilePath = dumpService.createDump();

						model.addAttribute("message", "Дамп успешно создан: " + dumpFilePath);
						model.addAttribute("downloadLink", "/tables/download?file=" + dumpFilePath); // Ссылка на скачивание
				} catch (Exception e) {
						model.addAttribute("error", "Ошибка создания дампа: " + e.getMessage());
				}
				return "backup_status";
		}

		// Восстановление из дампа
		@PostMapping("/restore")
		public String restoreDatabase(@RequestParam("dumpFilePath") String dumpFilePath, Model model) {
				try {
						dumpService.restoreDump(dumpFilePath);
						model.addAttribute("message", "База данных успешно восстановлена из дампа: " + dumpFilePath);
				} catch (Exception e) {
						model.addAttribute("error", "Ошибка восстановления базы данных: " + e.getMessage());
				}
				return "backup_status";
		}

		// Скачать бекап
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

		// Загрузка файла бекапа для восстановления
		@PostMapping("/upload")
		public String uploadBackup(@RequestParam("file") MultipartFile file, RedirectAttributes redirectAttributes) {
				try {
						if (file.isEmpty()) {
								redirectAttributes.addFlashAttribute("error", "Файл не выбран");
								return "redirect:/tables";
						}

						// Путь к папке для сохранения
						File targetFile = new File(System.getProperty("user.home") + File.separator + "Downloads" + File.separator + file.getOriginalFilename());
						file.transferTo(targetFile);

						// Проверка формата файла (только .sql)
						if (!targetFile.getName().endsWith(".sql")) {
								redirectAttributes.addFlashAttribute("error", "Неверный формат файла. Ожидается файл с расширением .sql.");
								return "redirect:/tables";
						}

						// Восстановление из загруженного дампа
						dumpService.restoreDump(targetFile.getAbsolutePath());

						redirectAttributes.addFlashAttribute("success", "База данных успешно восстановлена из загруженного бекапа!");
				} catch (IOException | InterruptedException e) {
						redirectAttributes.addFlashAttribute("error", "Ошибка восстановления базы данных: " + e.getMessage());
				}
				return "redirect:/tables";
		}
		@GetMapping("/{tableName}/export")
		public void exportTable(@PathVariable String tableName, HttpServletResponse response) throws IOException {
				// Устанавливаем тип контента для Excel
				response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
				response.setHeader("Content-Disposition", "attachment; filename=\"" + tableName + ".xlsx\"");

				// Получаем данные из базы данных
				List<Map<String, Object>> tableData = tableService.getTableData(tableName);

				// Создаем новый рабочий файл Excel
				XSSFWorkbook workbook = new XSSFWorkbook();
				Sheet sheet = workbook.createSheet("Data");

				if (!tableData.isEmpty()) {
						// Создаем строку заголовков
						Row headerRow = sheet.createRow(0);
						int headerCellIndex = 0;
						for (String key : tableData.get(0).keySet()) {
								Cell cell = headerRow.createCell(headerCellIndex++);
								cell.setCellValue(key);
						}

						// Заполняем таблицу данными
						for (int i = 0; i < tableData.size(); i++) {
								Row row = sheet.createRow(i + 1);
								int cellIndex = 0;
								for (Object value : tableData.get(i).values()) {
										Cell cell = row.createCell(cellIndex++);
										cell.setCellValue(value != null ? value.toString() : "");
								}
						}
				}

				// Отправляем файл на клиентскую сторону
				workbook.write(response.getOutputStream());
				workbook.close();
		}

}
