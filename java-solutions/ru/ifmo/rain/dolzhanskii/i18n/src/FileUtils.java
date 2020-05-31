package ru.ifmo.rain.dolzhanskii.i18n.src;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

public class FileUtils {
    private static void createParentDirectories(final String outputFileName) {
        Path outputFilePath;
        try {
            outputFilePath = Paths.get(outputFileName);
            Path parentDirectory = outputFilePath.getParent();
            if (parentDirectory != null) {
                Files.createDirectories(parentDirectory);
            }
        } catch (InvalidPathException | IOException e) {
            throw new RuntimeException("Unable to create output file parent directories '" + outputFileName + "'");
        }
    }

    static String readFile(final String inputFileName) throws IOException {
        return Files.readString(Paths.get(inputFileName));
    }

    static void writeFile(final String outputFileName, final String data) throws IOException {
        createParentDirectories(outputFileName);
        Files.writeString(Paths.get(outputFileName), data, StandardCharsets.UTF_8);
    }

    static String generateReport(final Locale inputLocale, final Locale outputLocale,
                                 final Map<TextStatistics.StatisticsType, TextStatistics.StatisticsData<?>> data,
                                 final String fileName)
            throws IOException {
        ResourceBundle bundle = ResourceBundle
                .getBundle("ru.ifmo.rain.dolzhanskii.i18n.resources.Bundle", outputLocale);

        final String sourceDir = "/home/oktet/IdeaProjects/JA/java-advanced-2020-solutions/java-solutions/ru/ifmo/rain/dolzhanskii/i18n/resources/";

        final String head = readFile(sourceDir + "/head-template.html");
        final String generatedHead = String.format(head,
                bundle.getString("title"));

        final String title = readFile(sourceDir + "/title-template.html");
        final String generatedTitle = String.format(title,
                bundle.getString("analyzedFile"),
                fileName);

        final String summary = readFile(sourceDir + "/summary-template.html");
        final String generatedSummary = String.format(summary,
                bundle.getString("summaryStats"),
                bundle.getString("sumSentence"),
                data.get(TextStatistics.StatisticsType.SENTENCE).getCountTotal(outputLocale),
                bundle.getString("sumLine"),
                data.get(TextStatistics.StatisticsType.LINE).getCountTotal(outputLocale),
                bundle.getString("sumWord"),
                data.get(TextStatistics.StatisticsType.WORD).getCountTotal(outputLocale),
                bundle.getString("sumNumber"),
                data.get(TextStatistics.StatisticsType.NUMBER).getCountTotal(outputLocale),
                bundle.getString("sumMoney"),
                data.get(TextStatistics.StatisticsType.MONEY).getCountTotal(outputLocale),
                bundle.getString("sumDate"),
                data.get(TextStatistics.StatisticsType.DATE).getCountTotal(outputLocale));

        final String section = readFile(sourceDir + "/section-template.html");
        List<String> generatedSections = Arrays.stream(TextStatistics.StatisticsType.values())
                .map(type -> {
                    final String sectionName = type.toString().charAt(0) + type.toString().substring(1).toLowerCase();
                    final TextStatistics.StatisticsData stats = data.get(type);
                    final Locale dataLocale = (type == TextStatistics.StatisticsType.MONEY)
                            ? inputLocale
                            : outputLocale;
                    final String meanKey;
                    final String mean;
                    switch (type) {
                        case SENTENCE:
                        case LINE:
                        case WORD: {
                            mean = stats.getMeanLength(outputLocale);
                            meanKey = "meanLen" + sectionName;
                            break;
                        }
                        default:
                            mean = stats.getMeanValue(dataLocale);
                            meanKey = "mean" + sectionName;
                    }

                    return String.format(section,
                            bundle.getString("stats" + sectionName),
                            bundle.getString("sum" + sectionName),
                            stats.getCountTotal(outputLocale),
                            stats.getCountUnique(outputLocale),
                            (stats.getCountUnique() % 10 == 1)
                                    ? bundle.getString("uniqueSingle")
                                    : bundle.getString("uniqueMultiple"),
                            bundle.getString("min" + sectionName),
                            stats.getMinValue(dataLocale),
                            bundle.getString("max" + sectionName),
                            stats.getMaxValue(dataLocale),
                            bundle.getString("minLen" + sectionName),
                            stats.getMinLength(outputLocale),
                            stats.getMinLengthValue(dataLocale),
                            bundle.getString("maxLen" + sectionName),
                            stats.getMaxLength(outputLocale),
                            stats.getMaxLengthValue(dataLocale),
                            bundle.getString(meanKey),
                            mean);
                }).collect(Collectors.toList());

        final String report = readFile(sourceDir + "/report-template.html");
        return String.format(report,
                generatedHead,
                generatedTitle,
                generatedSummary,
                generatedSections.get(0),
                generatedSections.get(1),
                generatedSections.get(2),
                generatedSections.get(3),
                generatedSections.get(4),
                generatedSections.get(5));
    }
}
