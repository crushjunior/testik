import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class JavaSchoolStarter {
    private List<Map<String, Object>> table = new ArrayList<>();
    private final String[] KEYS = {"id", "lastName", "age", "cost", "active"}; // keys

    public JavaSchoolStarter() {
    }

    public List<Map<String, Object>> execute(String request) throws Exception {
        String[] parts = request.trim().split("\\s+"); // получаем массив строк без пробелов
        String command = parts[0].toLowerCase();
        switch (command) {
            case "insert":
                return executeInsert(request.substring(command.length() + parts[1].length() + 1).trim()); // в параметр передается строка реквест, без первого и второго слова
            case "update":
                return executeUpdate(request.substring(command.length() + parts[1].length() + 1).trim());
           case "select":
               return executeSelect(request);
            case "delete":
                return executeDelete(request);
            default:
                throw new Exception("Unknown command: " + command);
        }
    }

    private List<Map<String, Object>> executeDelete(String request) {
        List<Map<String, Object>> deleteTable = new ArrayList<>(); // возвращаемая таблица
        List<String> requestList = requestToList(request); // реквест лист
        boolean isContainsAnd = false; // есть в запросе AND?


        // если where нет - возвращается вся исходная таблица
        if (requestList.size() == 1) {
            deleteTable.addAll(table);
            table.clear();
            return deleteTable;
        }

        List<String> requestAfterWhere = getListRequestAfterWhere(requestList); // лист запроса без where

        Map<String, List<String>> whereConditions = getMapWithConditions(requestAfterWhere, KEYS); // собираем мапу с условиями после where

        // проверка наличия AND в запросе
        if (requestAfterWhere.stream().anyMatch(r-> r.equalsIgnoreCase("and"))) {
            isContainsAnd = true;
        }

        // если мапа пустая, значит запрос не валидный
        if (whereConditions.isEmpty()) {
            throw new RuntimeException("Wrong request: " + request);
        }

        // проверка соответствия типов данных
        checkTypeData(whereConditions);

        // проверка исходной таблицы с параметрами запроса и заполнение возвращаемой таблицы
        checkConditionsAndFillingReturnTable(table, deleteTable, whereConditions, isContainsAnd);

        // удаляем из исходной таблицы строки
        for (Map<String, Object> keyValue : deleteTable) {
            table.remove(keyValue);
        }

        return deleteTable;
    }


    private List<Map<String, Object>> executeSelect(String request) {
        List<Map<String, Object>> selectTable = new ArrayList<>(); // возвращаемая таблица
        List<String> requestList = requestToList(request); // реквест лист
        boolean isContainsAnd = false; // есть в запросе AND?


        // если where нет - возвращается вся исходная таблица
        if (requestList.size() == 1) {
            selectTable.addAll(table);
            return selectTable;
        }

        List<String> requestAfterWhere = getListRequestAfterWhere(requestList); // лист запроса без where

        Map<String, List<String>> whereConditions = getMapWithConditions(requestAfterWhere, KEYS); // собираем мапу с условиями после where

        // проверка наличия AND в запросе
        if (requestAfterWhere.stream().anyMatch(r-> r.equalsIgnoreCase("and"))) {
            isContainsAnd = true;
        }

        // если мапа пустая, значит запрос не валидный
        if (whereConditions.isEmpty()) {
            throw new RuntimeException("Wrong request: " + request);
        }

        // проверка соответствия типов данных
        checkTypeData(whereConditions);

        // проверка исходной таблицы с параметрами запроса и заполнение возвращаемой таблицы
        checkConditionsAndFillingReturnTable(table, selectTable, whereConditions, isContainsAnd);
        return selectTable;
    }

    private List<Map<String, Object>> executeInsert(String request) throws Exception {
        Map<String, Object> newLine = parseRow(request);
        table.add(newLine);
        List<Map<String, Object>> insertedTable = new ArrayList<>(table);
        return insertedTable;
    }

    private List<Map<String, Object>> executeUpdate(String request) throws Exception {
        List<Map<String, Object>> updatedTable = new ArrayList<>();
        String[] requestParts = request.split("(?i) WHERE ", 2);
        Map<String, Object> newLine = parseRow(requestParts[0]);

        if (requestParts.length < 1) { // where нет, значит везде обновляем значения, где ключи совпадут
            for (Map<String, Object> map : table) {
                map.putAll(newLine);
            }
            updatedTable.addAll(table);
        } else {
            String whereClause = requestParts[1];
            for (Map<String, Object> row : table) {
                if (checkRequest(row, whereClause)) {
                    row.putAll(newLine);
                    updatedTable.add(row);
                }
            }
        }
        return updatedTable;
    }




    private Map<String, Object> parseRow(String request) {
        Map<String, Object> result = new HashMap<>(); // создается пока пустая мапа котрая будет возращена
        String[] parts = request.split(","); //  создаем массив строк разделив реквест через запятую
        for (String part : parts) { // проходимся по массиву в котором образовались пары ключ-значение в виде строк между которыми знак равно
            String[] keyValue = part.split("="); // создаем массив разделив предыдущий через равно, здесь остаются ключ и значение
            if (keyValue.length != 2) { // проверяем чтоб в массиве было 2 элемента
                return null;
            }
            String key = keyValue[0].replaceAll("'", "").trim(); // !!!!! .trim() обязательно создаем строку которая будет ключом, берем первый элемент пред массива, убираем одинарные ковычки
            int countMatchKeys = 0;
            for (int i = 0; i < KEYS.length; i++) {
                if (KEYS[i].equalsIgnoreCase(key)) {
                    countMatchKeys++;
                }
            }

            if (countMatchKeys == 0) {
                throw new RuntimeException("Invalid column name");
            }

            Object value = parseValue(keyValue[1]); // создаём объект распарсив второй элемент пред массива

            result.put(key, value); // в мапу вставляем получившиеся ключ и значение
        }
        return result;
    }

    private Object parseValue(String s) {
        if (s.matches("^'.*'$")) { // если начинается и заканчивается одинарной ковычкой, значит это строка
            return s.replaceAll("'", ""); // тогда возвращаем её убрав ковычки
        }

        if (s.matches("^true|false$")) { // если есть тру или фолс
            return Boolean.parseBoolean(s); // значит возвращаем булин
        }

        if (s.matches("^-?\\d+(\\.\\d+)?$")) { // если содержит только цифры и не более одной точки для десятичной части
            if (s.contains(".")) { // и если есть точка то делай дабл
                return Double.parseDouble(s);
            } else {
                return Long.parseLong(s); // иначе возращай лонг
            }
        }

        if (s == null) {
            return null;
        }
        // если ни к чему не относится, то ошибка
        throw new RuntimeException("Invalid value: " + s);
    }

   private boolean checkRequest(Map<String, Object> existingRow, String whereClause) {
       if (whereClause.isEmpty() || whereClause == null) {
           return true;
       }

       Pattern pattern = Pattern.compile("('?\\w+'?)\\s*(=|!=|>|<|>=|<=)\\s*(.*)");
       Matcher matcher = pattern.matcher(whereClause);

       if (!matcher.matches()) {
           throw new IllegalArgumentException("Wrong request: " + whereClause);
       }

       String columnName = matcher.group(1);
       if (columnName.contains("'")) {
           columnName = columnName.replaceAll("'", "");
       }
       String operator = matcher.group(2);
       Object comparableValue = parseValue(matcher.group(3));

       int countMatchKey = 0;
       for (String key : existingRow.keySet()) {
           if (key.equalsIgnoreCase(columnName)) {
               countMatchKey++;
           }
       }

       if (countMatchKey == 0) {
           throw new IllegalArgumentException("This column doesn't exist: " + columnName);
       }

       return toCompare(comparableValue, existingRow.get(columnName), operator);
   }

    private static List<String> requestToList (String request) {
        String changeRequest = request
                .replaceAll("\\s*>=\\s*", " >= ")
                .replaceAll("\\s*[‘’',]>\\s*", " > ")
                .replaceAll("\\s*[‘’',]!=\\s*", " != ")
                .replaceAll("\\s,\\s*", ", ")
                .replaceAll("\\s*<=\\s*", " <= ")
                .replaceAll("\\s*[‘’',]=\\s*", " = ")
                .replaceAll("\\s*[‘’',]<\\s*", " < ")
                .replaceAll("'", "");

        List<String> requestList = Arrays.stream(changeRequest.split(" ")).collect(Collectors.toList());
        return requestList;
    }

    private static void checkConditionsAndFillingReturnTable(List<Map<String, Object>> table,  List<Map<String, Object>> selectTable,  Map<String, List<String>> whereConditions, boolean isContainsAnd) {
        for (Map<String, Object> keyValue : table) {

            int countMatches = 0;
            int countKeys = 0;

            for (String condition : whereConditions.keySet()) {
                if (keyValue.containsKey(condition)) {
                    countKeys++;
                    String operator = whereConditions.get(condition).get(0);
                    String valueMatch = String.valueOf(whereConditions.get(condition).get(1));

                    if (operator.equals("=")) {
                        if (valueMatch.equalsIgnoreCase(keyValue.get(condition).toString())) {
                            countMatches++;
                        }
                    }

                    if (operator.equals(">")) {
                        double resultTable = Double.parseDouble(String.valueOf(keyValue.get(condition) == null ? 0 : Double.parseDouble(String.valueOf(keyValue.get(condition)))));
                        double resultMatch = Double.parseDouble(valueMatch);
                        if (resultTable > resultMatch) {
                            countMatches++;
                        }
                    }

                    if (operator.equals("<")) {
                        double resultTable = Double.parseDouble(String.valueOf(keyValue.get(condition) == null ? 0 : Double.parseDouble(String.valueOf(keyValue.get(condition)))));
                        double resultMatch = Double.parseDouble(valueMatch);
                        if (resultTable < resultMatch) {
                            countMatches++;
                        }
                    }

                    if (operator.equals(">=")) {
                        double resultTable = Double.parseDouble(String.valueOf(keyValue.get(condition) == null ? 0 : Double.parseDouble(String.valueOf(keyValue.get(condition)))));
                        double resultMatch = Double.parseDouble(valueMatch);
                        if (resultTable >= resultMatch) {
                            countMatches++;
                        }
                    }

                    if (operator.equals("<=")) {
                        double resultTable = Double.parseDouble(String.valueOf(keyValue.get(condition) == null ? 0 : Double.parseDouble(String.valueOf(keyValue.get(condition)))));
                        double resultMatch = Double.parseDouble(valueMatch);
                        if (resultTable <= resultMatch) {
                            countMatches++;
                        }
                    }

                    if (operator.equals("!=")) {
                        if (!keyValue.get(condition).equals(valueMatch)) {
                            countMatches++;
                        }
                    }

                    if (operator.equalsIgnoreCase("like")) {
                        String valueFromTable = keyValue.get(condition).toString();
                        String valueFromConditionsWithPercent = whereConditions.get(condition).get(1);
                        String valueFromConditionsWithoutPercent = valueFromConditionsWithPercent.replaceAll("%", "");

                        if (valueFromConditionsWithPercent.startsWith("%") && valueFromConditionsWithPercent.endsWith("%") && valueFromTable.contains(valueFromConditionsWithoutPercent)) {
                            countMatches++;
                        }

                        if (valueFromConditionsWithPercent.startsWith("%") && valueFromTable.endsWith(valueFromConditionsWithoutPercent)) {
                            countMatches++;
                        }

                        if (valueFromConditionsWithPercent.endsWith("%") && valueFromTable.startsWith(valueFromConditionsWithoutPercent)) {
                            countMatches++;
                        }
                    }

                    if (operator.equalsIgnoreCase("ilike")) {
                        String valueFromTableI = keyValue.get(condition).toString().toLowerCase();
                        String valueFromConditionsWithPercentI = whereConditions.get(condition).get(1);
                        String valueFromConditionsWithoutPercentI = valueFromConditionsWithPercentI.replaceAll("%", "").toLowerCase();

                        if (valueFromConditionsWithPercentI.startsWith("%") && valueFromConditionsWithPercentI.endsWith("%") && valueFromTableI.contains(valueFromConditionsWithoutPercentI)) {
                            countMatches++;
                        }

                        if (valueFromConditionsWithPercentI.startsWith("%") && valueFromTableI.endsWith(valueFromConditionsWithoutPercentI)) {
                            countMatches++;
                        }

                        if (valueFromConditionsWithPercentI.endsWith("%") && valueFromTableI.startsWith(valueFromConditionsWithoutPercentI)) {
                            countMatches++;
                        }
                    }
                }
            }

            if (countMatches == countKeys && isContainsAnd) {
                selectTable.add(keyValue);
            }

            if (!isContainsAnd && countMatches > 0) {
                selectTable.add(keyValue);
            }
        }
    }

    private static void checkTypeData(Map<String, List<String>> whereConditions) throws RuntimeException {
        for (String key : whereConditions.keySet()) {
            String value = whereConditions.get(key).get(1).replaceAll("%", "");

            if (key.equalsIgnoreCase("lastName")) {
                if (!Pattern.matches("[a-zA-Zа-яА-Я]+", value)) {
                    throw new RuntimeException("Wrong data type for column " + key + ": " + value);
                }
            }

            if (key.equalsIgnoreCase("id") || key.equalsIgnoreCase("age")) {
                try {
                    Long.parseLong(value);
                } catch (NumberFormatException e) {
                    throw new RuntimeException("Wrong data type for column " + key + ": " + value);
                }
            }

            if (key.equalsIgnoreCase("cost")) {
                try {
                    Double.parseDouble(value);
                } catch (NumberFormatException e) {
                    throw new RuntimeException("Wrong data type for column " + key + ": " + value);
                }
            }

            if (key.equalsIgnoreCase("active")) {
                if (!value.equalsIgnoreCase("true") && !value.equalsIgnoreCase("false")) {
                    throw new RuntimeException("Wrong data type for column " + key + ": " + value);
                }
            }
        }
    }

    private static List<String> getListRequestAfterWhere(List<String> requestList) {
        int indexWithWhere = 0;
        for (int i = 0; i < requestList.size(); i++) {
            if (requestList.get(i).equalsIgnoreCase("WHERE")) {
                indexWithWhere = i;
                break;
            }
        }
        return requestList.stream().skip(indexWithWhere + 1).collect(Collectors.toList());
    }

    private static Map<String, List<String>> getMapWithConditions(List<String> requestAfterWhere, String[] keys) {
        Map<String, List<String>> whereConditions = new HashMap<>();
        for (int i = 0; i < requestAfterWhere.size(); i++) {
            for (int j = 0; j < keys.length; j++) {
                if (requestAfterWhere.get(i).equalsIgnoreCase(keys[j])) {
                    ArrayList<String> conditions = new ArrayList<>();
                    conditions.add(requestAfterWhere.get(i + 1));
                    conditions.add(requestAfterWhere.get(i + 2));
                    whereConditions.put(keys[j], conditions);
                }
            }
        }
        return whereConditions;
    }

    private boolean toCompare(Object comparableValue, Object originalValue, String operator) {
        if (originalValue == null) {
            return !operator.equals("=");
        }

        if (comparableValue == null) {
            return !operator.equals("!=");
        }

        int compareResult;
        if (originalValue instanceof Comparable && comparableValue instanceof Comparable) {
            compareResult = ((Comparable) originalValue).compareTo(comparableValue);
        } else {
            throw new IllegalArgumentException(originalValue + " is no match for a " + comparableValue);
        }

        switch (operator) { // проверка по оператору, возвращает тру, если всё совпало, фолс, если нет, либо ошибку, если оператор передан не верно
            case "=":
                return compareResult == 0;
            case "!=":
                return compareResult != 0;
            case ">":
                return compareResult > 0;
            case "<":
                return compareResult < 0;
            case ">=":
                return compareResult >= 0;
            case "<=":
                return compareResult <= 0;
            default:
                throw new IllegalArgumentException("Invalid operator: " + operator);
        }
    }
}

