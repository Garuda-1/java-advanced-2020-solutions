package ru.ifmo.rain.dolzhanskii.i18n.test;

import org.junit.Assert;
import org.junit.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.ifmo.rain.dolzhanskii.i18n.FileUtils;
import ru.ifmo.rain.dolzhanskii.i18n.TextStatistics;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

@RunWith(Parameterized.class)
public class TextStatisticsTest extends Assert {
    private Locale locale;

    @Parameterized.Parameters(name = "Locale: {0}")
    public static Collection languages() {
        return Arrays.asList(new Object[][] {
                {"ru-RU"},
                {"en-US"},
                {"ar-AE"}
        });
    }

    public TextStatisticsTest(final String languageTag) {
        this.locale = new Locale.Builder().setLanguageTag(languageTag).build();
    }

    private void validateStatistics(
            final Map<TextStatistics.StatisticsType, TextStatistics.StatisticsData<?>> statistics,
            final String testName) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {

        ResourceBundle bundle = ResourceBundle
                .getBundle("ru.ifmo.rain.dolzhanskii.i18n.test.resources." + testName, locale);

        final Class<?> clazz = TextStatistics.StatisticsData.class;
        final List<String> fieldNames = Arrays.stream(clazz.getDeclaredFields()).map(Field::getName)
                .collect(Collectors.toList());
        for (TextStatistics.StatisticsType type : TextStatistics.StatisticsType.values()) {
            final TextStatistics.StatisticsData<?> data = statistics.get(type);
            for (String fieldName : fieldNames) {
                try {
                    Method fieldGetter = clazz.getMethod("get" + Character.toUpperCase(fieldName.charAt(0))
                            + fieldName.substring(1));
                    final Object gotObject = fieldGetter.invoke(data);
                    final String gotString = (gotObject == null) ? "null" : gotObject.toString();

                    try {
                        final String expectedString = bundle.getString(type.toString().toLowerCase() + "_" +
                                fieldName);
                        assertEquals(expectedString, gotString);
                    } catch (final MissingResourceException e) {
                        // Ignored.
                    }
                } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                    throw e;
                }
            }
        }
    }

    private void testRoutine(final String testName) throws IOException, NoSuchMethodException, IllegalAccessException,
            InvocationTargetException {
        final String text = FileUtils.readResourceFile(TextStatisticsTest.class, testName + "_" +
                locale.getLanguage() + "_" + locale.getCountry() + ".txt");
        final Map<TextStatistics.StatisticsType, TextStatistics.StatisticsData<?>> statistics
                = TextStatistics.getStatistics(text, locale);
        validateStatistics(statistics, testName);
    }

    @Test
    @DisplayName("Simple test")
    public void simpleTest() throws IOException, NoSuchMethodException, IllegalAccessException,
            InvocationTargetException {
        testRoutine("simple");
    }

    @Test
    @DisplayName("Poem test")
    public void poemTest() throws IOException, NoSuchMethodException, IllegalAccessException,
            InvocationTargetException {
        testRoutine("poem");
    }

    @Test
    @DisplayName("Biography test")
    public void biographyTest() throws IOException, NoSuchMethodException, IllegalAccessException,
            InvocationTargetException {
        testRoutine("biography");
    }

    @Test
    @DisplayName("Dates test")
    public void datesTest() throws IOException, NoSuchMethodException, IllegalAccessException,
            InvocationTargetException {
        testRoutine("dates");
    }

    @Test
    @DisplayName("Numerical test")
    public void numericalTest() throws IOException, NoSuchMethodException, IllegalAccessException,
            InvocationTargetException {
        testRoutine("numerical");
    }

    @Test
    @DisplayName("Corners")
    public void cornersTest() throws IOException, NoSuchMethodException, IllegalAccessException,
            InvocationTargetException {
        testRoutine("corners");
    }
}
