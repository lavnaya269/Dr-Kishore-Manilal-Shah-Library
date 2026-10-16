
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LibraryApiServer {

    private static HttpServer server;

    public static void main(String[] args) throws Exception {
        server = HttpServer.create(new InetSocketAddress(8080), 0);

        server.createContext("/", LibraryApiServer::home);
        server.createContext("/api/books", LibraryApiServer::books);
        server.createContext("/api/students", LibraryApiServer::students);
        server.createContext("/api/issues", LibraryApiServer::issues);
        server.createContext("/api/returns", LibraryApiServer::returns);
        server.createContext("/api/history", LibraryApiServer::history);

        server.setExecutor(null);
        server.start();

        System.out.println("======================================");
        System.out.println(" SIWS College Library Portal");
        System.out.println(" Server running at http://localhost:8080");
        System.out.println(" Press Ctrl+C to stop the server.");
        System.out.println("======================================");
    }

    // ---------------- HOME PAGE ----------------

    private static void home(HttpExchange ex) throws IOException {
        addCorsHeaders(ex);

        if ("OPTIONS".equalsIgnoreCase(ex.getRequestMethod())) {
            send(ex, 204, "");
            return;
        }

        String path = ex.getRequestURI().getPath();

        if ("/".equals(path) || "/index.html".equals(path)) {
            serveFile(ex, Paths.get("index.html"),
                    "text/html; charset=UTF-8");
            return;
        }

        if ("/siws-logo.png".equals(path)) {
            serveFile(ex, Paths.get("siws-logo.png"), "image/png");
            return;
        }

        sendError(ex, 404, "Page not found.");
    }

    private static void serveFile(
            HttpExchange ex, Path file, String contentType)
            throws IOException {

        if (!Files.exists(file)) {
            sendError(ex, 404, file.getFileName() + " was not found.");
            return;
        }

        byte[] content = Files.readAllBytes(file);
        ex.getResponseHeaders().set("Content-Type", contentType);
        ex.sendResponseHeaders(200, content.length);

        try (OutputStream os = ex.getResponseBody()) {
            os.write(content);
        }
    }

    // ---------------- BOOKS API ----------------

    private static void books(HttpExchange ex) throws IOException {
        addCorsHeaders(ex);

        if (handleOptions(ex)) return;

        String method = ex.getRequestMethod();

        try (Connection con = DBConnection.getConnection()) {

            if ("GET".equalsIgnoreCase(method)) {
                String sql =
                        "SELECT book_id, title, author, category, quantity " +
                        "FROM books ORDER BY book_id";

                List<Map<String, Object>> result = new ArrayList<>();

                try (PreparedStatement ps = con.prepareStatement(sql);
                     ResultSet rs = ps.executeQuery()) {

                    while (rs.next()) {
                        Map<String, Object> book = new LinkedHashMap<>();
                        book.put("book_id", rs.getInt("book_id"));
                        book.put("title", rs.getString("title"));
                        book.put("author", rs.getString("author"));
                        book.put("category", rs.getString("category"));
                        book.put("quantity", rs.getInt("quantity"));
                        result.add(book);
                    }
                }

                send(ex, 200, toJson(result));
                return;
            }

            if ("POST".equalsIgnoreCase(method)) {
                Map<String, String> data;

                try {
                    data = readRequestData(ex);
                } catch (Exception e) {
                    sendError(ex, 400,
                            "Could not read book request: " + errorMessage(e));
                    return;
                }

                String title = data.get("title");
                String author = data.get("author");
                String category = data.get("category");
                int quantity = parseInt(data.get("quantity"), 1);

                if (isBlank(title) || isBlank(author)) {
                    sendError(ex, 400,
                            "Book title and author are required.");
                    return;
                }

                if (quantity < 0) {
                    sendError(ex, 400, "Quantity cannot be negative.");
                    return;
                }

                String sql =
                        "INSERT INTO books (title, author, category, quantity) " +
                        "VALUES (?, ?, ?, ?)";

                try (PreparedStatement ps = con.prepareStatement(
                        sql, Statement.RETURN_GENERATED_KEYS)) {

                    ps.setString(1, title.trim());
                    ps.setString(2, author.trim());
                    ps.setString(3, emptyToNull(category));
                    ps.setInt(4, quantity);
                    ps.executeUpdate();

                    int id = 0;

                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        if (keys.next()) id = keys.getInt(1);
                    }

                    Map<String, Object> response = new LinkedHashMap<>();
                    response.put("message", "Book added successfully.");
                    response.put("book_id", id);
                    send(ex, 201, toJson(response));
                }

                return;
            }

            sendError(ex, 405, "Method not allowed.");

        } catch (Exception e) {
            e.printStackTrace();
            sendError(ex, 500, errorMessage(e));
        }
    }

    // ---------------- STUDENTS API ----------------

    private static void students(HttpExchange ex) throws IOException {
        addCorsHeaders(ex);

        if (handleOptions(ex)) return;

        String method = ex.getRequestMethod();

        try (Connection con = DBConnection.getConnection()) {

            if ("GET".equalsIgnoreCase(method)) {
                String sql =
                        "SELECT student_id, name, roll_number, department, phone " +
                        "FROM students ORDER BY student_id";

                List<Map<String, Object>> result = new ArrayList<>();

                try (PreparedStatement ps = con.prepareStatement(sql);
                     ResultSet rs = ps.executeQuery()) {

                    while (rs.next()) {
                        Map<String, Object> student = new LinkedHashMap<>();
                        student.put("student_id", rs.getInt("student_id"));
                        student.put("name", rs.getString("name"));
                        student.put("roll_number", rs.getString("roll_number"));
                        student.put("department", rs.getString("department"));
                        student.put("phone", rs.getString("phone"));
                        result.add(student);
                    }
                }

                send(ex, 200, toJson(result));
                return;
            }

            if ("POST".equalsIgnoreCase(method)) {
                Map<String, String> data;

                try {
                    data = readRequestData(ex);
                } catch (Exception e) {
                    sendError(ex, 400,
                            "Could not read student request: " + errorMessage(e));
                    return;
                }

                String name = firstValue(data, "name", "student_name");
                String roll = firstValue(data, "roll_number", "rollNumber");
                String department = data.get("department");
                String phone = data.get("phone");

                if (isBlank(name) || isBlank(roll)) {
                    sendError(ex, 400,
                            "Student name and roll number are required.");
                    return;
                }

                String sql =
                        "INSERT INTO students " +
                        "(name, roll_number, department, phone) " +
                        "VALUES (?, ?, ?, ?)";

                try (PreparedStatement ps = con.prepareStatement(
                        sql, Statement.RETURN_GENERATED_KEYS)) {

                    ps.setString(1, name.trim());
                    ps.setString(2, roll.trim());
                    ps.setString(3, emptyToNull(department));
                    ps.setString(4, emptyToNull(phone));
                    ps.executeUpdate();

                    int id = 0;

                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        if (keys.next()) id = keys.getInt(1);
                    }

                    Map<String, Object> response = new LinkedHashMap<>();
                    response.put("message", "Student added successfully.");
                    response.put("student_id", id);
                    send(ex, 201, toJson(response));
                }

                return;
            }

            sendError(ex, 405, "Method not allowed.");

        } catch (Exception e) {
            e.printStackTrace();
            sendError(ex, 500, errorMessage(e));
        }
    }

    // ---------------- ISSUE BOOK API ----------------

    private static void issues(HttpExchange ex) throws IOException {
        addCorsHeaders(ex);

        if (handleOptions(ex)) return;

        String method = ex.getRequestMethod();

        if ("GET".equalsIgnoreCase(method)) {
            try (Connection con = DBConnection.getConnection()) {
                String sql =
                        "SELECT ib.issue_id, ib.student_id, s.name, " +
                        "s.roll_number, ib.book_id, b.title, " +
                        "ib.issue_date, ib.return_date, ib.status " +
                        "FROM issued_books ib " +
                        "JOIN students s ON ib.student_id = s.student_id " +
                        "JOIN books b ON ib.book_id = b.book_id " +
                        "WHERE ib.status = 'Issued' " +
                        "ORDER BY ib.issue_id DESC";

                List<Map<String, Object>> result = new ArrayList<>();

                try (PreparedStatement ps = con.prepareStatement(sql);
                     ResultSet rs = ps.executeQuery()) {

                    while (rs.next()) {
                        Map<String, Object> item = new LinkedHashMap<>();
                        item.put("issue_id", rs.getInt("issue_id"));
                        item.put("student_id", rs.getInt("student_id"));
                        item.put("student_name", rs.getString("name"));
                        item.put("name", rs.getString("name"));
                        item.put("roll_number", rs.getString("roll_number"));
                        item.put("book_id", rs.getInt("book_id"));
                        item.put("book_title", rs.getString("title"));
                        item.put("title", rs.getString("title"));
                        item.put("issue_date",
                                rs.getDate("issue_date").toString());

                        Date returnDate = rs.getDate("return_date");
                        item.put("return_date",
                                returnDate == null ? null : returnDate.toString());

                        item.put("status", rs.getString("status"));
                        result.add(item);
                    }
                }

                send(ex, 200, toJson(result));
                return;

            } catch (Exception e) {
                e.printStackTrace();
                sendError(ex, 500, errorMessage(e));
                return;
            }
        }

        if (!"POST".equalsIgnoreCase(method)) {
            sendError(ex, 405, "Method not allowed.");
            return;
        }

        Map<String, String> data;

        try {
            data = readRequestData(ex);
        } catch (Exception e) {
            sendError(ex, 400,
                    "Could not read issue request: " + errorMessage(e));
            return;
        }

        String studentValue = firstValue(
                data, "student_id", "studentId", "student");
        String bookValue = firstValue(
                data, "book_id", "bookId", "book");
        String dateValue = firstValue(data, "issue_date", "issueDate");

        int studentId = parseInt(studentValue, -1);
        int bookId = parseInt(bookValue, -1);

        if (studentId <= 0 || bookId <= 0) {
            sendError(ex, 400,
                    "Valid student_id and book_id are required. " +
                    "Received student_id=" + studentValue +
                    ", book_id=" + bookValue);
            return;
        }

        LocalDate issueDate;

        try {
            issueDate = isBlank(dateValue)
                    ? LocalDate.now()
                    : LocalDate.parse(dateValue.trim());
        } catch (Exception e) {
            sendError(ex, 400,
                    "Invalid issue date. Use YYYY-MM-DD format.");
            return;
        }

        try (Connection con = DBConnection.getConnection()) {
            con.setAutoCommit(false);

            try {
                try (PreparedStatement ps = con.prepareStatement(
                        "SELECT student_id FROM students WHERE student_id = ?")) {
                    ps.setInt(1, studentId);

                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            con.rollback();
                            sendError(ex, 400, "Student not found.");
                            return;
                        }
                    }
                }

                try (PreparedStatement ps = con.prepareStatement(
                        "UPDATE books SET quantity = quantity - 1 " +
                        "WHERE book_id = ? AND quantity > 0")) {
                    ps.setInt(1, bookId);

                    if (ps.executeUpdate() == 0) {
                        con.rollback();

                        try (PreparedStatement check = con.prepareStatement(
                                "SELECT book_id FROM books WHERE book_id = ?")) {
                            check.setInt(1, bookId);

                            try (ResultSet rs = check.executeQuery()) {
                                if (!rs.next()) {
                                    sendError(ex, 400, "Book not found.");
                                } else {
                                    sendError(ex, 400,
                                            "No copies of this book are available.");
                                }
                            }
                        }
                        return;
                    }
                }

                String sql =
                        "INSERT INTO issued_books " +
                        "(student_id, book_id, issue_date, status) " +
                        "VALUES (?, ?, ?, 'Issued')";

                int issueId;

                try (PreparedStatement ps = con.prepareStatement(
                        sql, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setInt(1, studentId);
                    ps.setInt(2, bookId);
                    ps.setDate(3, Date.valueOf(issueDate));
                    ps.executeUpdate();

                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        if (!keys.next()) {
                            throw new Exception(
                                    "Could not obtain the issue record ID.");
                        }
                        issueId = keys.getInt(1);
                    }
                }

                con.commit();

                Map<String, Object> response = new LinkedHashMap<>();
                response.put("message", "Book issued successfully.");
                response.put("issue_id", issueId);
                response.put("student_id", studentId);
                response.put("book_id", bookId);
                response.put("issue_date", issueDate.toString());
                response.put("status", "Issued");

                send(ex, 201, toJson(response));

            } catch (Exception e) {
                try {
                    con.rollback();
                } catch (Exception rollbackError) {
                    rollbackError.printStackTrace();
                }
                throw e;

            } finally {
                try {
                    con.setAutoCommit(true);
                } catch (Exception ignored) {
                    // Connection closes automatically.
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            sendError(ex, 500, errorMessage(e));
        }
    }

    // ---------------- RETURN BOOK API ----------------

    private static void returns(HttpExchange ex) throws IOException {
        addCorsHeaders(ex);

        if (handleOptions(ex)) return;

        if (!"POST".equalsIgnoreCase(ex.getRequestMethod())) {
            sendError(ex, 405, "Use POST to return a book.");
            return;
        }

        Map<String, String> data;

        try {
            data = readRequestData(ex);
        } catch (Exception e) {
            e.printStackTrace();
            sendError(ex, 400,
                    "Could not read return request: " + errorMessage(e));
            return;
        }

        int issueId = parseInt(
                firstValue(data, "issue_id", "issueId"), -1);

        if (issueId <= 0) {
            sendError(ex, 400, "A valid issue_id is required.");
            return;
        }

        try (Connection con = DBConnection.getConnection()) {
            con.setAutoCommit(false);

            try {
                int bookId;

                try (PreparedStatement ps = con.prepareStatement(
                        "SELECT book_id FROM issued_books " +
                        "WHERE issue_id = ? AND status = 'Issued' FOR UPDATE")) {
                    ps.setInt(1, issueId);

                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            con.rollback();
                            sendError(ex, 404,
                                    "Active issue record not found. " +
                                    "Check the issue ID or whether it was already returned.");
                            return;
                        }
                        bookId = rs.getInt("book_id");
                    }
                }

                int updated;

                try (PreparedStatement ps = con.prepareStatement(
                        "UPDATE issued_books SET status = 'Returned', " +
                        "return_date = ? " +
                        "WHERE issue_id = ? AND status = 'Issued'")) {
                    ps.setDate(1, Date.valueOf(LocalDate.now()));
                    ps.setInt(2, issueId);
                    updated = ps.executeUpdate();
                }

                if (updated != 1) {
                    con.rollback();
                    sendError(ex, 409,
                            "This book has already been returned.");
                    return;
                }

                try (PreparedStatement ps = con.prepareStatement(
                        "UPDATE books SET quantity = quantity + 1 " +
                        "WHERE book_id = ?")) {
                    ps.setInt(1, bookId);

                    if (ps.executeUpdate() != 1) {
                        throw new Exception(
                                "Book record not found while processing return.");
                    }
                }

                con.commit();

                Map<String, Object> response = new LinkedHashMap<>();
                response.put("message", "Book returned successfully.");
                response.put("issue_id", issueId);
                response.put("status", "Returned");

                send(ex, 200, toJson(response));

            } catch (Exception e) {
                try {
                    con.rollback();
                } catch (Exception rollbackError) {
                    rollbackError.printStackTrace();
                }
                throw e;

            } finally {
                try {
                    con.setAutoCommit(true);
                } catch (Exception ignored) {
                    // Connection closes automatically.
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            sendError(ex, 500, errorMessage(e));
        }
    }

    // ---------------- TRANSACTION HISTORY API ----------------

    private static void history(HttpExchange ex) throws IOException {
        addCorsHeaders(ex);

        if (handleOptions(ex)) return;

        if (!"GET".equalsIgnoreCase(ex.getRequestMethod())) {
            sendError(ex, 405,
                    "Use GET to view transaction history.");
            return;
        }

        try (Connection con = DBConnection.getConnection()) {
            String sql =
                    "SELECT ib.issue_id, ib.student_id, s.name, " +
                    "s.roll_number, ib.book_id, b.title, " +
                    "ib.issue_date, ib.return_date, ib.status " +
                    "FROM issued_books ib " +
                    "JOIN students s ON ib.student_id = s.student_id " +
                    "JOIN books b ON ib.book_id = b.book_id " +
                    "ORDER BY ib.issue_id DESC";

            List<Map<String, Object>> result = new ArrayList<>();

            try (PreparedStatement ps = con.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {

                while (rs.next()) {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("issue_id", rs.getInt("issue_id"));
                    item.put("student_id", rs.getInt("student_id"));
                    item.put("student_name", rs.getString("name"));
                    item.put("name", rs.getString("name"));
                    item.put("roll_number", rs.getString("roll_number"));
                    item.put("book_id", rs.getInt("book_id"));
                    item.put("book_title", rs.getString("title"));
                    item.put("title", rs.getString("title"));
                    item.put("issue_date",
                            rs.getDate("issue_date").toString());

                    Date returnDate = rs.getDate("return_date");
                    item.put("return_date",
                            returnDate == null ? null : returnDate.toString());

                    item.put("status", rs.getString("status"));
                    result.add(item);
                }
            }

            send(ex, 200, toJson(result));

        } catch (Exception e) {
            e.printStackTrace();
            sendError(ex, 500, errorMessage(e));
        }
    }

    // ---------------- REQUEST HELPERS ----------------

    private static boolean handleOptions(HttpExchange ex)
            throws IOException {
        if ("OPTIONS".equalsIgnoreCase(ex.getRequestMethod())) {
            send(ex, 204, "");
            return true;
        }
        return false;
    }

    private static void addCorsHeaders(HttpExchange ex) {
        Headers headers = ex.getResponseHeaders();
        headers.set("Access-Control-Allow-Origin", "*");
        headers.set("Access-Control-Allow-Methods",
                "GET, POST, PUT, DELETE, OPTIONS");
        headers.set("Access-Control-Allow-Headers",
                "Content-Type, Authorization");
        headers.set("Cache-Control", "no-store");
    }

    private static Map<String, String> readRequestData(
            HttpExchange ex) throws IOException {

        String body;

        try (InputStream is = ex.getRequestBody()) {
            body = new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }

        Map<String, String> result = new LinkedHashMap<>();

        if (body.trim().isEmpty()) return result;

        String contentType = ex.getRequestHeaders()
                .getFirst("Content-Type");

        if (contentType != null &&
                contentType.toLowerCase().contains("application/json")) {
            return parseSimpleJson(body);
        }

        return parseForm(body);
    }

    private static Map<String, String> parseForm(String body)
            throws IOException {
        Map<String, String> result = new LinkedHashMap<>();

        for (String pair : body.split("&")) {
            if (pair.isEmpty()) continue;

            String[] parts = pair.split("=", 2);

            String key = URLDecoder.decode(
                    parts[0], StandardCharsets.UTF_8);

            String value = parts.length > 1
                    ? URLDecoder.decode(parts[1], StandardCharsets.UTF_8)
                    : "";

            result.put(key, value);
        }

        return result;
    }

    // Supports JSON strings, numbers, booleans and null.
    private static Map<String, String> parseSimpleJson(String json) {
        Map<String, String> result = new LinkedHashMap<>();

        Pattern pattern = Pattern.compile(
                "\"([^\"]+)\"\\s*:\\s*" +
                "(?:\"((?:\\\\.|[^\"\\\\])*)\"|(null)|(-?\\d+(?:\\.\\d+)?|true|false))"
        );

        Matcher matcher = pattern.matcher(json);

        while (matcher.find()) {
            String key = matcher.group(1);
            String value;

            if (matcher.group(3) != null) {
                value = "";
            } else if (matcher.group(2) != null) {
                value = unescapeJsonString(matcher.group(2));
            } else {
                value = matcher.group(4);
            }

            result.put(key, value);
        }

        return result;
    }

    private static String unescapeJsonString(String value) {
        return value
                .replace("\\\"", "\"")
                .replace("\\\\", "\\")
                .replace("\\n", "\n")
                .replace("\\r", "\r")
                .replace("\\t", "\t");
    }

    private static String firstValue(
            Map<String, String> data, String... keys) {
        for (String key : keys) {
            String value = data.get(key);
            if (value != null && !value.trim().isEmpty()) {
                return value.trim();
            }
        }
        return null;
    }

    private static int parseInt(String value, int defaultValue) {
        try {
            if (value == null || value.trim().isEmpty()) {
                return defaultValue;
            }
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static String emptyToNull(String value) {
        return isBlank(value) ? null : value.trim();
    }

    private static String errorMessage(Exception e) {
        String message = e.getMessage();

        if (message == null || message.trim().isEmpty()) {
            return e.getClass().getSimpleName();
        }

        return message;
    }

    private static void sendError(
            HttpExchange ex, int status, String message)
            throws IOException {
        Map<String, Object> error = new LinkedHashMap<>();
        error.put("error", message);
        send(ex, status, toJson(error));
    }

    private static void send(
            HttpExchange ex, int status, String response)
            throws IOException {

        byte[] bytes = response.getBytes(StandardCharsets.UTF_8);

        ex.getResponseHeaders().set(
                "Content-Type", "application/json; charset=UTF-8");

        if (status == 204) {
            ex.sendResponseHeaders(status, -1);
            ex.close();
            return;
        }

        ex.sendResponseHeaders(status, bytes.length);

        try (OutputStream os = ex.getResponseBody()) {
            os.write(bytes);
        }
    }

    // ---------------- JSON RESPONSE CONVERTER ----------------

    private static String toJson(Object value) {
        if (value == null) return "null";

        if (value instanceof String) {
            return "\"" + escapeJson((String) value) + "\"";
        }

        if (value instanceof Number || value instanceof Boolean) {
            return value.toString();
        }

        if (value instanceof Map) {
            StringBuilder json = new StringBuilder("{");
            boolean first = true;

            Map<?, ?> map = (Map<?, ?>) value;

            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (!first) json.append(",");
                first = false;

                json.append(toJson(String.valueOf(entry.getKey())));
                json.append(":");
                json.append(toJson(entry.getValue()));
            }

            json.append("}");
            return json.toString();
        }

        if (value instanceof Iterable) {
            StringBuilder json = new StringBuilder("[");
            boolean first = true;

            for (Object item : (Iterable<?>) value) {
                if (!first) json.append(",");
                first = false;
                json.append(toJson(item));
            }

            json.append("]");
            return json.toString();
        }

        return toJson(String.valueOf(value));
    }

    private static String escapeJson(String value) {
        StringBuilder result = new StringBuilder();

        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);

            switch (c) {
                case '"':
                    result.append("\\\"");
                    break;
                case '\\':
                    result.append("\\\\");
                    break;
                case '\b':
                    result.append("\\b");
                    break;
                case '\f':
                    result.append("\\f");
                    break;
                case '\n':
                    result.append("\\n");
                    break;
                case '\r':
                    result.append("\\r");
                    break;
                case '\t':
                    result.append("\\t");
                    break;
                default:
                    if (c < 0x20) {
                        result.append(String.format("\\u%04x", (int) c));
                    } else {
                        result.append(c);
                    }
            }
        }

        return result.toString();
    }
}
