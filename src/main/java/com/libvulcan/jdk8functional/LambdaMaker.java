package com.libvulcan.jdk8functional;

import lombok.Data;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;

import java.lang.invoke.*;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

@Slf4j
public class LambdaMaker {

    @Data
    @Accessors(chain = true)
    static class Student {
        private String name;
        private String gender;
        private int age;

        private List<String> teacherNames;
    }

    public static void main(String[] args) throws Throwable {
        // new obj
        final Student lily = new Student().setName("Lily").setGender("female").setAge(22);
        log.info("initialize student obj: {}", lily.toString());

        // eg: normal code
        // add new teacher name to lily
        addTeacherName(lily, Student::getTeacherNames, Student::setTeacherNames);
        // print name list
        outputName(lily::getTeacherNames);


        // eg: lambda maker
        // make lambda
        Function<Student, List<String>> getFunc = makeGetFunc();
        BiConsumer<Student, List<String>> setFunc = makeSetFunc();
        // add new teacher name to lily use lambda
        addTeacherName(lily, getFunc, setFunc);

        // make lambda
        Supplier<List<String>> getTeacherNameFunc = makeGetTeacherNameFunc(lily);
        // print name list
        outputName(getTeacherNameFunc);
    }

    /**
     * add new teacher name to {@link Student#teacherNames}
     * @param student object
     * @param getter string list getter
     * @param setter string list setter
     */
    public static void addTeacherName(Student student, Function<Student, List<String>> getter,
                                      BiConsumer<Student, List<String>> setter) {
        // generate random teacher's name.
        Random random = new Random();
        String teacherName = "teacher" + random.nextInt(1000);

        // add teacher's name
        final List<String> apply = getter.apply(student);
        Optional.ofNullable(apply).map((list -> {
            list.add(teacherName);
            return list;
        })).orElseGet(() -> {
            final List<String> list = new ArrayList<>();
            setter.accept(student, list);
            list.add(teacherName);
            return list;
        });
    }

    /**
     * output {@link Student#teacherNames}
     * @param output string
     */
    public static void outputName(Supplier<List<String>> output) {
        log.info("functional - outputName: {}", output.get());
    }


    /**
     * make {@link Function} for {@link Student#getTeacherNames()}
     * create a {@link Function} through {@link LambdaMetafactory#metafactory}
     * @return getter
     * @throws Throwable exception
     */
    public static Function<Student, List<String>> makeGetFunc() throws Throwable {

        final Method getter = Student.class.getMethod("getTeacherNames");
        final MethodHandles.Lookup lookup = MethodHandles.lookup();

        final MethodHandle getMethodHandle = lookup.unreflect(getter);

        final CallSite getCallSite = LambdaMetafactory.metafactory(
                lookup,
                "apply",
                MethodType.methodType(Function.class),
                MethodType.methodType(Object.class, Object.class),
                getMethodHandle,
                MethodType.methodType(List.class, Student.class)
        );

        @SuppressWarnings("unchecked")
        final Function<Student, List<String>> func =
                (Function<Student, List<String>>) getCallSite.getTarget().invokeExact();

        return func;
    }

    /**
     * make {@link BiConsumer} for {@link Student#setTeacherNames}}
     * create a {@link BiConsumer} through {@link LambdaMetafactory#metafactory}
     * @return getter
     * @throws Throwable exception
     */
    public static BiConsumer<Student, List<String>> makeSetFunc() throws Throwable {
        final Method setter = Student.class.getMethod("setTeacherNames", List.class);
        final MethodHandles.Lookup lookup = MethodHandles.lookup();

        final MethodHandle setMethodHandle = lookup.unreflect(setter);

        final CallSite setCallSite = LambdaMetafactory.metafactory(
                lookup,
                "accept",
                MethodType.methodType(BiConsumer.class),
                MethodType.methodType(void.class, Object.class, Object.class),
                setMethodHandle,
                MethodType.methodType(void.class, Student.class, List.class)
        );

        @SuppressWarnings("unchecked")
        final BiConsumer<Student, List<String>> func =
                (BiConsumer<Student, List<String>>) setCallSite.getTarget().invokeExact();

        return func;
    }

    /**
     * make {@link Supplier} for {@link Student#getTeacherNames()}
     * create a {@link Supplier} through {@link LambdaMetafactory#metafactory}
     * @return getter
     * @throws Throwable exception
     */
    public static Supplier<List<String>> makeGetTeacherNameFunc(Student student) throws Throwable {
        final Method getter = Student.class.getMethod("getTeacherNames");
        final MethodHandles.Lookup lookup = MethodHandles.lookup();

        final MethodHandle getMethodHandle = lookup.unreflect(getter);

        final CallSite getCallSite = LambdaMetafactory.metafactory(
                lookup,
                "get",
                MethodType.methodType(Supplier.class, Student.class),
                MethodType.methodType(Object.class),
                getMethodHandle,
                MethodType.methodType(List.class)
        );

        @SuppressWarnings("unchecked")
        final Supplier<List<String>> supplier =
                (Supplier<List<String>>) getCallSite.getTarget().invokeExact(student);

        return supplier;
    }
}
