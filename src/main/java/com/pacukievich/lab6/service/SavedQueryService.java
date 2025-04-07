package com.pacukievich.lab6.service;

import com.pacukievich.lab6.model.SavedQuery;
import com.pacukievich.lab6.repository.SavedQueryRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class SavedQueryService {

		private final JdbcTemplate jdbcTemplate;
		private final SavedQueryRepository repository;

		public SavedQueryService(JdbcTemplate jdbcTemplate, SavedQueryRepository repository) {
				this.jdbcTemplate = jdbcTemplate;
				this.repository = repository;
		}

		public List<SavedQuery> getAllQueries() {
				return repository.findAll();
		}

		public SavedQuery getQueryById(Long id) {
				return repository.findById(id).orElseThrow();
		}

		public List<Map<String, Object>> executeSql(String sql) {
				return jdbcTemplate.queryForList(sql);
		}
}