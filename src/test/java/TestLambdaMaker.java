import com.libvulcan.util.JsonUtil;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;

import java.lang.invoke.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public class TestLambdaMaker {

    @Data
    @Accessors(chain = true)
    static class Student {
        /**
         * range 1-6
         */
        private int grade;
        private String name;
    }

    /**
     * There are 6 properties({@link List<Student>}) in class {@link StudentStatistics} called list1、list2, ... list6;<br/>
     * listN means the student's grade is N, eg:<br/>
     * list3 means 3rd grade
     */
    @Data
    @Accessors
    static class StudentStatistics {
        private List<Student> list1;
        private List<Student> list2;
        private List<Student> list3;
        private List<Student> list4;
        private List<Student> list5;
        private List<Student> list6;
    }

    /**
     * generate random {@link Student} list
     * @param n count
     * @return {@link Student} list
     */
    private List<Student> makeNRandomStudent(int n) {
        Random random = new Random();
        return Stream.generate(() ->
                        new Student().setGrade(random.nextInt(6) + 1).setName("student" + random.nextInt(1000)))
                .limit(n).collect(Collectors.toList());
    }

    @Test
    public void TestNormalCase() {
        final StudentStatistics studentStatistics = new StudentStatistics();
        final List<Student> students = makeNRandomStudent(100);
        for (Student student : students) {
            if (student.getGrade() == 1) {
                if (studentStatistics.getList1() == null) {
                    studentStatistics.setList1(new ArrayList<>());
                }
                studentStatistics.getList1().add(student);
            }
            if (student.getGrade() == 2) {
                if (studentStatistics.getList2() == null) {
                    studentStatistics.setList2(new ArrayList<>());
                }
                studentStatistics.getList2().add(student);
            }
            if (student.getGrade() == 3) {
                if (studentStatistics.getList3() == null) {
                    studentStatistics.setList3(new ArrayList<>());
                }
                studentStatistics.getList3().add(student);
            }
            if (student.getGrade() == 4) {
                if (studentStatistics.getList4() == null) {
                    studentStatistics.setList4(new ArrayList<>());
                }
                studentStatistics.getList4().add(student);
            }
            if (student.getGrade() == 5) {
                if (studentStatistics.getList5() == null) {
                    studentStatistics.setList5(new ArrayList<>());
                }
                studentStatistics.getList5().add(student);
            }
            if (student.getGrade() == 6) {
                if (studentStatistics.getList6() == null) {
                    studentStatistics.setList6(new ArrayList<>());
                }
                studentStatistics.getList6().add(student);
            }
        }

        log.info(JsonUtil.toJsonString(studentStatistics));
    }

    @Test
    public void TestLambdaCase() {
        final List<Student> students = makeNRandomStudent(100);

        final Map<Integer, BiConsumer<Student, StudentStatistics>> map = new HashMap<>();

        final Field[] fields = StudentStatistics.class.getDeclaredFields();

        for (Field field : fields) {
            field.setAccessible(true);
            final String fieldName = field.getName();
            final char key = fieldName.charAt(fieldName.length() - 1);

            @SuppressWarnings("unchecked")
            BiConsumer<Student, StudentStatistics> consumer = (student, studentStatistics) -> {
                try {
                    Optional.ofNullable((List<Student>) field.get(studentStatistics)).map((list) -> {
                        list.add(student);
                        return list;
                    }).orElseGet(() -> {
                        final List<Student> list = new ArrayList<>();
                        try {
                            field.set(studentStatistics, list);
                        } catch (IllegalAccessException e) {
                            e.printStackTrace();
                        }
                        list.add(student);
                        return list;
                    });
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            };

            map.put(Integer.parseInt(String.valueOf(key)), consumer);
        }

        StudentStatistics studentStatistics = new StudentStatistics();

        for (Student student : students) {
            map.get(student.grade).accept(student, studentStatistics);
        }

        log.info(JsonUtil.toJsonString(studentStatistics));
    }

    @Test
    public void TestLambdaEnhancedCase() {

        // bridge mode
        @Data
        class AddStudentClass {
            private Function<StudentStatistics, List<Student>> getter;
            private BiConsumer<StudentStatistics, List<Student>> setter;

            public void addStudent(Student student, StudentStatistics studentStatistics) {
                Optional.ofNullable(getter.apply(studentStatistics)).map((list) -> {
                    list.add(student);
                    return list;
                }).orElseGet(() -> {
                    final List<Student> list = new ArrayList<>();
                    list.add(student);
                    setter.accept(studentStatistics, list);
                    return list;
                });
            }
        }

        final Map<Integer, AddStudentClass> map = new HashMap<>();

        final MethodHandles.Lookup lookup = MethodHandles.lookup();

        Arrays.stream(StudentStatistics.class.getDeclaredMethods())
                .filter(method -> method.getName().startsWith("get") || method.getName().startsWith("set"))
                .collect(Collectors.groupingBy(method -> method.getName().substring(3)))
                .forEach((k, v) -> {
                    final AddStudentClass addStudentClass = new AddStudentClass();
                    map.put(Integer.parseInt(k.substring(k.length() - 1)), addStudentClass);
                    v.forEach(method -> {
                        if (method.getName().contains("get")) {
                            try {
                                MethodHandle methodHandle = lookup.unreflect(method);
                                CallSite callSite = LambdaMetafactory.metafactory(
                                        lookup,
                                        "apply",
                                        MethodType.methodType(Function.class),
                                        MethodType.methodType(Object.class, Object.class),
                                        methodHandle,
                                        MethodType.methodType(List.class, StudentStatistics.class)
                                );
                                @SuppressWarnings("unchecked")
                                final Function<StudentStatistics, List<Student>> getter = (Function<StudentStatistics, List<Student>>) callSite.getTarget().invokeExact();
                                addStudentClass.setGetter(getter);
                            } catch (Throwable e) {
                                e.printStackTrace();
                            }
                        } else {
                            try {
                                MethodHandle methodHandle = lookup.unreflect(method);
                                CallSite callSite = LambdaMetafactory.metafactory(
                                        lookup,
                                        "accept",
                                        MethodType.methodType(BiConsumer.class),
                                        MethodType.methodType(void.class, Object.class, Object.class),
                                        methodHandle,
                                        MethodType.methodType(void.class, StudentStatistics.class, List.class)
                                );
                                @SuppressWarnings("unchecked")
                                final BiConsumer<StudentStatistics, List<Student>> setter = (BiConsumer<StudentStatistics, List<Student>>) callSite.getTarget().invokeExact();
                                addStudentClass.setSetter(setter);
                            } catch (Throwable e) {
                                e.printStackTrace();
                            }
                        }
                    });
                });

        final List<Student> students = makeNRandomStudent(100);

        final StudentStatistics studentStatistics = new StudentStatistics();

        for (Student it : students) {
            map.get(it.getGrade()).addStudent(it, studentStatistics);
        }

        log.info(JsonUtil.toJsonString(studentStatistics));
    }
}
