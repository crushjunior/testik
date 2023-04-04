import java.util.List;
import java.util.Map;

public class Main {
    public static void main(String[] args) {

        JavaSchoolStarter starter = new JavaSchoolStarter();

        List<Map<String, Object>> result1;
        try {
            result1 = starter.execute("INSERT VALUES 'lastName'='Федоров', 'id'=3, 'age'=10, 'active'=false/true);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        System.out.println("Добавить Федорова: " + result1);
        System.out.println();

        List<Map<String, Object>> result2;
        try {
            result2 = starter.execute("INSERT VALUES 'lastName'='Иванов', 'id'=2, 'age'=20, 'active'=true, 'cost'=11.2");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        System.out.println("Добавить Иванова: " + result2);
        System.out.println();

        List<Map<String, Object>> resultUp;
        try {
            resultUp = starter.execute("UPDATE VALUES 'cost'=9.9 where 'active'=true");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        System.out.println("Обновить данные там, где active=true: " + resultUp);
        System.out.println();


        List<Map<String, Object>> select;
        try {
            select = starter.execute("SELECT WHERE 'age'<=30 and 'lastName' like 'Ива%'");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        System.out.println("Выбрать согласно критериям: " + select);
        System.out.println();

        List<Map<String, Object>> delete;
        try {
            delete = starter.execute("DELETE WHERE 'id'=3 and 'lastName' ilike '%РОВ'");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        System.out.println("Удалить Федорова: " + delete);
        System.out.println();

        List<Map<String, Object>> selectAll;
        try {
            selectAll = starter.execute("SELECT");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        System.out.println("Показать все содержимое таблицы: " + selectAll);
        System.out.println();

    }
}
