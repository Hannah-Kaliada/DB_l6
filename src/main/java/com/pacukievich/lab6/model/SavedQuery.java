package com.pacukievich.lab6.model;

import jakarta.persistence.*;

@Entity
@Table(name = "saved_queries")
public class SavedQuery {

		@Id
		@GeneratedValue(strategy = GenerationType.IDENTITY)
		private Long id;

		@Column(nullable = false, unique = true)
		private String name;

		@Column(name = "query_text", nullable = false, columnDefinition = "TEXT")
		private String queryText;

		// Геттеры и сеттеры

		public Long getId() {
				return id;
		}

		public void setId(Long id) {
				this.id = id;
		}

		public String getName() {
				return name;
		}

		public void setName(String name) {
				this.name = name;
		}

		public String getQueryText() {
				return queryText;
		}

		public void setQueryText(String queryText) {
				this.queryText = queryText;
		}
}