package com.pacukievich.lab6.service;

import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

@Service
public class DumpService {

		private static final String HOST = "localhost";
		private static final String PORT = "5432";
		private static final String DATABASE_NAME = "voenkomat2";
		private static final String USER = "postgres";
		private static final String PASSWORD = "postgres";
		private static final String PG_DUMP_PATH = "/Applications/Postgres.app/Contents/Versions/latest/bin/pg_dump";
		private static final String PSQL_PATH = "/Applications/Postgres.app/Contents/Versions/latest/bin/psql";
		private static final String BACKUP_DIR = Paths.get(System.getProperty("user.home"), "Downloads").toString();

		public String createDump() throws IOException, InterruptedException {
				String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
				String plainDumpFilePath = Paths.get(BACKUP_DIR, "backup_" + timestamp + ".sql").toString();
				createDatabaseDump(plainDumpFilePath, "p");
				return plainDumpFilePath;
		}

		private void createDatabaseDump(String dumpFilePath, String format) throws IOException, InterruptedException {
				if (dumpFilePath == null || dumpFilePath.isEmpty()) {
						throw new IllegalArgumentException("Путь к файлу дампа не может быть пустым.");
				}

				ProcessBuilder processBuilder = new ProcessBuilder(
								PG_DUMP_PATH,
								"--inserts",
								"-h", HOST,
								"-p", PORT,
								"-U", USER,
								"-F", format,
								"-v",
								"-f", dumpFilePath,
								DATABASE_NAME
				);

				Map<String, String> environment = processBuilder.environment();
				environment.put("PGPASSWORD", PASSWORD);

				System.out.println("Запуск команды: " + String.join(" ", processBuilder.command()));

				Process process = processBuilder.start();
				logProcessOutput(process);
				int exitCode = process.waitFor();

				if (exitCode != 0) {
						throw new RuntimeException("Ошибка создания дампа, код: " + exitCode);
				}
		}

		public void restoreDump(String dumpFilePath) throws IOException, InterruptedException {
				File dumpFile = new File(dumpFilePath);
				if (!dumpFile.exists()) {
						throw new IllegalArgumentException("Файл дампа не найден: " + dumpFilePath);
				}

				ProcessBuilder processBuilder = new ProcessBuilder(
								PSQL_PATH,
								"-h", HOST,
								"-p", PORT,
								"-U", USER,
								"-d", DATABASE_NAME,
								"-f", dumpFilePath
				);

				Map<String, String> environment = processBuilder.environment();
				environment.put("PGPASSWORD", PASSWORD);

				System.out.println("Запуск команды восстановления: " + String.join(" ", processBuilder.command()));

				Process process = processBuilder.start();
				logProcessOutput(process);
				int exitCode = process.waitFor();

				if (exitCode != 0) {
						throw new RuntimeException("Ошибка восстановления дампа, код: " + exitCode);
				}
		}

		private void logProcessOutput(Process process) throws IOException {
				try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
				     BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
						String line;
						while ((line = reader.readLine()) != null) {
								System.out.println("[INFO] " + line);
						}
						while ((line = errorReader.readLine()) != null) {
								System.err.println("[ERROR] " + line);
						}
				}
		}
}
