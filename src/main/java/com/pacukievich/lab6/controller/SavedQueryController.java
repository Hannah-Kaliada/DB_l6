package com.pacukievich.lab6.controller;

import com.pacukievich.lab6.model.SavedQuery;
import com.pacukievich.lab6.repository.SavedQueryRepository;
import com.pacukievich.lab6.service.SavedQueryService;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.springframework.ui.Model;

@Controller
@RequestMapping("/queries")
public class SavedQueryController {

		private final JdbcTemplate jdbcTemplate;
		private final SavedQueryRepository repository;

		private final SavedQueryService queryService;

		public SavedQueryController(SavedQueryService queryService, JdbcTemplate jdbcTemplate, SavedQueryRepository repository) {
				this.queryService = queryService;
				this.jdbcTemplate = jdbcTemplate;
				this.repository = repository;
		}

		@GetMapping
		public String listQueries(Model model) {
				List<SavedQuery> queries = queryService.getAllQueries();
				model.addAttribute("queries", queries);
				return "queries/list";
		}

		@GetMapping("/{id}")
		public String executeQuery(@PathVariable Long id, Model model) {
				SavedQuery query = queryService.getQueryById(id);
				List<Map<String, Object>> result = queryService.executeSql(query.getQueryText());

				model.addAttribute("queryName", query.getName());
				model.addAttribute("result", result);
				return "queries/result";
		}
		@PostMapping("/execute-temp")
		public String executeTempQuery(@RequestParam String queryText, Model model) {
				List<Map<String, Object>> result = jdbcTemplate.queryForList(queryText);
				model.addAttribute("result", result);
				model.addAttribute("queryName", "Временный запрос");
				return "queries/result";
		}
		@PostMapping("/execute")
		public String saveAndExecuteQuery(@RequestParam String name,
		                                  @RequestParam String queryText,
		                                  Model model) {
				// Сохраняем запрос в БД
				SavedQuery savedQuery = new SavedQuery();
				savedQuery.setName(name);
				savedQuery.setQueryText(queryText);
				repository.save(savedQuery);

				// Выполняем его
				List<Map<String, Object>> result = jdbcTemplate.queryForList(queryText);
				model.addAttribute("result", result);
				model.addAttribute("queryName", name);
				return "queries/result";
		}
		@PostMapping("/export")
		public ResponseEntity<byte[]> exportToExcel(@RequestBody ExportRequest exportRequest) throws IOException {
				List<Map<String, String>> tableData = exportRequest.getData();

				// Создаем новый Excel файл
				Workbook workbook = new XSSFWorkbook();
				Sheet sheet = workbook.createSheet("Query Results");

				if (!tableData.isEmpty()) {
						// Добавляем заголовки
						Row headerRow = sheet.createRow(0);
						int colIndex = 0;
						for (String header : tableData.get(0).keySet()) {
								Cell cell = headerRow.createCell(colIndex++);
								cell.setCellValue(header);
						}

						// Добавляем данные
						int rowIndex = 1;
						for (Map<String, String> rowData : tableData) {
								Row row = sheet.createRow(rowIndex++);
								colIndex = 0;
								for (String value : rowData.values()) {
										Cell cell = row.createCell(colIndex++);
										cell.setCellValue(value);
								}
						}
				}

				// Записываем в выходной поток
				ByteArrayOutputStream out = new ByteArrayOutputStream();
				workbook.write(out);
				workbook.close();

				// Создаем ResponseEntity с данными для скачивания
				byte[] byteArray = out.toByteArray();
				HttpHeaders headers = new HttpHeaders();
				headers.add("Content-Disposition", "attachment; filename=query_result.xlsx");

				return ResponseEntity.ok()
								.headers(headers)
								.body(byteArray);
		}

		// Класс для обработки JSON запроса
		public static class ExportRequest {
				private List<Map<String, String>> data;

				public List<Map<String, String>> getData() {
						return data;
				}

				public void setData(List<Map<String, String>> data) {
						this.data = data;
				}
		}
}