package com.pacukievich.lab6.model;

public class FieldRequest {
		private String name;
		private String type;
		private boolean notNull;
		private boolean unique;
		private boolean primaryKey;
		private String defaultValue;

		public String getName() { return name; }
		public void setName(String name) { this.name = name; }

		public String getType() { return type; }
		public void setType(String type) { this.type = type; }

		public boolean isNotNull() { return notNull; }
		public void setNotNull(boolean notNull) { this.notNull = notNull; }

		public boolean isUnique() { return unique; }
		public void setUnique(boolean unique) { this.unique = unique; }

		public boolean isPrimaryKey() { return primaryKey; }
		public void setPrimaryKey(boolean primaryKey) { this.primaryKey = primaryKey; }

		public String getDefaultValue() { return defaultValue; }
		public void setDefaultValue(String defaultValue) { this.defaultValue = defaultValue; }
}
