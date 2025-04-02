package com.pacukievich.lab6.controller;

import com.pacukievich.lab6.model.TableRequest;
import com.pacukievich.lab6.model.FieldRequest;
import com.pacukievich.lab6.service.TableService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.ui.Model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/tables")
public class TableController {
		private final TableService tableService;

		public TableController(TableService tableService) {
				this.tableService = tableService;
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
		// Отображает список всех таблиц
		@GetMapping
		public String showTables(Model model) {
				List<String> tables = tableService.getAllTables();
				model.addAttribute("tables", tables);
				return "tables";
		}

		// Удаляет выбранную таблицу
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

}