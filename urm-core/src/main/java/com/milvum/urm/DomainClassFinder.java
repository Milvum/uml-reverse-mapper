package com.milvum.urm;

import com.google.common.collect.*;
import org.reflections.*;
import org.reflections.scanners.ResourcesScanner;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.reflections.util.FilterBuilder;

import java.net.URLClassLoader;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class DomainClassFinder {

    private static final Logger logger = Logger.getLogger(DomainClassFinder.class.getName());

    private static final String URM_PACKAGE = "com.milvum.urm";
    public static boolean ALLOW_FINDING_INTERNAL_CLASSES;

    public static ClassLoader[] classLoaders;

    public static List<Class<?>> findClasses(final List<String> packages, List<String> ignores, final ClassLoader classLoader) {
        return packages.stream()
                .map(packageName -> getClasses(classLoader, packageName))
                .flatMap(Collection::stream)
                .filter(DomainClassFinder::isNotPackageInfo)
                .filter(DomainClassFinder::isNotAnonymousClass)
                .filter((Class<?> clazz) -> !ignores.contains(clazz.getName()) && !ignores.contains(clazz.getSimpleName())
                        && !ignores.contains(clazz.getPackage().getName()))
                .sorted(Comparator.comparing(Class::getName))
                .collect(Collectors.toList());
    }

    private static boolean isNotPackageInfo(Class<?> clazz) {
        return !clazz.getSimpleName().equals("package-info");
    }

    private static boolean isNotAnonymousClass(Class<?> clazz) {
        return !clazz.getSimpleName().equals("");
    }

    private static Set<Class<?>> getClasses(ClassLoader classLoader, String packageName) {
        List<ClassLoader> classLoadersList = new LinkedList<>();
        classLoadersList.add(ClasspathHelper.contextClassLoader());
        classLoadersList.add(ClasspathHelper.staticClassLoader());
        if (classLoader != null) {
            classLoadersList.add(classLoader);
        }

        classLoaders = classLoadersList.toArray(new ClassLoader[0]);

        FilterBuilder filter = new FilterBuilder()
                .include(FilterBuilder.prefix(packageName));
        if (!isAllowFindingInternalClasses()) {
            filter.exclude(FilterBuilder.prefix(URM_PACKAGE));
        }

        Reflections reflections = new Reflections(new ConfigurationBuilder()
                .setScanners(new SubTypesScanner(false /* don't exclude Object.class */),
                        new ResourcesScanner())
                .setUrls(ClasspathHelper.forPackage(packageName, classLoaders))
                .filterInputsBy(filter)
                .addClassLoaders(classLoadersList)
        );

        Multimap<String, String> mmap = reflections.getStore().get(SubTypesScanner.class.getSimpleName());
        return Sets.newHashSet(ReflectionUtils.forNames(mmap.values(), classLoadersList.toArray(new ClassLoader[0])));
    }

    public static boolean isAllowFindingInternalClasses() {
        return ALLOW_FINDING_INTERNAL_CLASSES |= Boolean.parseBoolean(
                System.getProperty("com.milvum.urm.DomainClassFinder.allowFindingInternalClasses", "false"));
    }

    private DomainClassFinder() {
        // private constructor for utility class
    }
}
