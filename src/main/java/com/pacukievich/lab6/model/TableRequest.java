package com.pacukievich.lab6.model;

import java.util.List;

public class TableRequest {
		private String tableName;
		private List<FieldRequest> fields;


		public String getTableName() {
				return tableName;
		}

		public void setTableName(String tableName) {
				this.tableName = tableName;
		}

		public List<FieldRequest> getFields() {
				return fields;
		}

		public void setFields(List<FieldRequest> fields) {
				this.fields = fields;
		}
}