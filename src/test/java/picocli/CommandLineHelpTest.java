/*
   Copyright 2017 Remko Popma

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */
package picocli;

import org.junit.Test;
import picocli.CommandLine.Help;
import picocli.CommandLine.Help.Column;
import picocli.CommandLine.Help.TextTable;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Command;

import java.awt.Point;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.lang.String;
import java.lang.reflect.Field;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.List;

import static java.lang.String.format;
import static org.junit.Assert.*;
import static picocli.CommandLine.Help.Column.Overflow.*;

/**
 * Tests for picoCLI's "Usage" help functionality.
 */
public class CommandLineHelpTest {
    private static final String LINESEP = System.getProperty("line.separator");
    private static String usageString(Object annotatedObject) throws UnsupportedEncodingException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        CommandLine.usage(annotatedObject, new PrintStream(baos, true, "UTF8"));
        String result = baos.toString("UTF8");
        return result;
    }
    private static Field field(Class<?> cls, String fieldName) throws NoSuchFieldException {
        return cls.getDeclaredField(fieldName);
    }
    private static Field[] fields(Class<?> cls, String... fieldNames) throws NoSuchFieldException {
        Field[] result = new Field[fieldNames.length];
        for (int i = 0; i < fieldNames.length; i++) {
            result[i] = cls.getDeclaredField(fieldNames[i]);
        }
        return result;
    }

    @Test
    public void testUsageAnnotationDetailedUsageWithoutDefaultValue() throws Exception {
        @CommandLine.Command(showDefaultValues = false)
        class Params {
            @Option(names = {"-f", "--file"}, required = true, description = "the file to use") File file;
        }
        String result = usageString(new Params());
        assertEquals(format("" +
                        "Usage: <main class> -f=<file>%n" +
                        "  -f, --file=<file>           the file to use%n",
                ""), result);
    }

    @Test
    public void testUsageAnnotationDetailedUsageWithDefaultValue() throws Exception {
        @CommandLine.Command()
        class Params {
            @Option(names = {"-f", "--file"}, required = true, description = "the file to use") File file = new File("theDefault.txt");
        }
        String result = usageString(new Params());
        assertEquals(format("" +
                        "Usage: <main class> -f=<file>%n" +
                        "  -f, --file=<file>           the file to use%n" +
                        "                              Default: theDefault.txt%n"), result);
    }

    @Test
    public void testUsageSeparatorWithoutDefault() throws Exception {
        @Command(showDefaultValues = false)
        class Params {
            @Option(names = {"-f", "--file"}, required = true, description = "the file to use") File file = new File("def.txt");
        }
        String result = usageString(new Params());
        assertEquals(format("" +
                        "Usage: <main class> -f=<file>%n" +
                        "  -f, --file=<file>           the file to use%n",
                ""), result);
    }

    @Test
    public void testUsageSeparator() throws Exception {
        class Params {
            @Option(names = {"-f", "--file"}, required = true, description = "the file to use") File file = new File("def.txt");
        }
        String result = usageString(new Params());
        assertEquals(format("" +
                        "Usage: <main class> -f=<file>%n" +
                        "  -f, --file=<file>           the file to use%n" +
                        "                              Default: def.txt%n",
                ""), result);
    }

    @Test
    public void testShortestFirstComparator_sortsShortestFirst() {
        String[] values = {"12345", "12", "123", "123456", "1", "", "1234"};
        Arrays.sort(values, new Help.ShortestFirst());
        String[] expected = {"", "1", "12", "123", "1234", "12345", "123456"};
        assertArrayEquals(expected, values);
    }

    @Test
    public void testShortestFirstComparator_sortsDeclarationOrderIfEqualLength() {
        String[] values = {"-d", "-", "-a", "--alpha", "--b", "--a", "--beta"};
        Arrays.sort(values, new Help.ShortestFirst());
        String[] expected = {"-", "-d", "-a", "--b", "--a", "--beta", "--alpha"};
        assertArrayEquals(expected, values);
    }

    @Test
    public void testSortByShortestOptionNameComparator() throws Exception {
        class App {
            @Option(names = {"-t", "--aaaa"}) boolean aaaa;
            @Option(names = {"--bbbb", "-k"}) boolean bbbb;
            @Option(names = {"-c", "--cccc"}) boolean cccc;
        }
        Field[] fields = fields(App.class, "aaaa", "bbbb", "cccc"); // -tkc
        Arrays.sort(fields, new Help.SortByShortestOptionNameAlphabetically());
        Field[] expected = fields(App.class, "cccc", "bbbb", "aaaa"); // -ckt
        assertArrayEquals(expected, fields);
    }

    @Test
    public void testSortByOptionArityAndNameComparator_sortsByMaxThenMinThenName() throws Exception {
        class App {
            @Option(names = {"-t", "--aaaa"}) boolean tImplicitArity0;
            @Option(names = {"-e", "--EEE"}, arity = "1") boolean explicitArity1;
            @Option(names = {"--bbbb", "-k"}) boolean kImplicitArity0;
            @Option(names = {"--AAAA", "-a"}) int aImplicitArity1;
            @Option(names = {"--BBBB", "-b"}) String[] bImplicitArity0_n;
            @Option(names = {"--ZZZZ", "-z"}, arity = "1..3") String[] zExplicitArity1_3;
            @Option(names = {"-f", "--ffff"}) boolean fImplicitArity0;
        }
        Field[] fields = fields(App.class, "tImplicitArity0", "explicitArity1", "kImplicitArity0",
                "aImplicitArity1", "bImplicitArity0_n", "zExplicitArity1_3", "fImplicitArity0");
        Arrays.sort(fields, new Help.SortByOptionArityAndNameAlphabetically());
        Field[] expected = fields(App.class,
                "fImplicitArity0",
                "kImplicitArity0",
                "tImplicitArity0",
                "aImplicitArity1",
                "explicitArity1",
                "zExplicitArity1_3",
                "bImplicitArity0_n");
        assertArrayEquals(expected, fields);
    }

    @Test
    public void testCreateMinimalOptionRenderer_ReturnsMinimalOptionRenderer() {
        assertEquals(Help.MinimalOptionRenderer.class, Help.createMinimalOptionRenderer().getClass());
    }

    @Test
    public void testMinimalOptionRenderer_rendersFirstDeclaredOptionNameAndDescription() {
        class Example {
            @Option(names = {"---long", "-L"}, description = "long description") String longField;
            @Option(names = {"-b", "-a", "--alpha"}, description = "other") String otherField;
        }
        Help.IOptionRenderer renderer = Help.createMinimalOptionRenderer();
        Help help = new Help(new Example());
        Help.IValueLabelRenderer parameterRenderer = help.createDefaultValueLabelRenderer();
        Field field = help.optionFields.get(0);
        String[][] row1 = renderer.render(field.getAnnotation(Option.class), field, parameterRenderer);
        assertEquals(1, row1.length);
        assertArrayEquals(new String[]{"---long=<longField>", "long description"}, row1[0]);

        field = help.optionFields.get(1);
        String[][] row2 = renderer.render(field.getAnnotation(Option.class), field, parameterRenderer);
        assertEquals(1, row2.length);
        assertArrayEquals(new String[]{"-b=<otherField>", "other"}, row2[0]);
    }

    @Test
    public void testCreateDefaultOptionRenderer_ReturnsDefaultOptionRenderer() {
        assertEquals(Help.DefaultOptionRenderer.class, new Help(new UsageDemo()).createDefaultOptionRenderer().getClass());
    }

    @Test
    public void testDefaultOptionRenderer_rendersShortestOptionNameThenOtherOptionNamesAndDescription() {
        class Example {
            @Option(names = {"---long", "-L"}, description = "long description") String longField;
            @Option(names = {"-b", "-a", "--alpha"}, description = "other") String otherField = "abc";
        }
        Help help = new Help(new Example());
        Help.IOptionRenderer renderer = help.createDefaultOptionRenderer();
        Help.IValueLabelRenderer parameterRenderer = help.createDefaultValueLabelRenderer();
        Field field = help.optionFields.get(0);
        String[][] row1 = renderer.render(field.getAnnotation(Option.class), field, parameterRenderer);
        assertEquals(2, row1.length);
        assertArrayEquals(Arrays.toString(row1[0]), new String[]{"", "-L", ",", "---long=<longField>", "long description"}, row1[0]);
        assertArrayEquals(Arrays.toString(row1[1]), new String[]{"", "", "", "", "Default: null"}, row1[1]);

        field = help.optionFields.get(1);
        String[][] row2 = renderer.render(field.getAnnotation(Option.class), field, parameterRenderer);
        assertEquals(2, row2.length);
        assertArrayEquals(Arrays.toString(row2[0]), new String[]{"", "-b", ",", "-a, --alpha=<otherField>", "other"}, row2[0]);
        assertArrayEquals(Arrays.toString(row2[1]), new String[]{"", "", "", "", "Default: abc"}, row2[1]);
    }

    @Test
    public void testDefaultOptionRenderer_rendersSpecifiedMarkerForRequiredOptionsWithDefault() {
        @Command(requiredOptionMarker = '*')
        class Example {
            @Option(names = {"-b", "-a", "--alpha"}, required = true, description = "other") String otherField ="abc";
        }
        Help help = new Help(new Example());
        Help.IOptionRenderer renderer = help.createDefaultOptionRenderer();
        Help.IValueLabelRenderer parameterRenderer = help.createDefaultValueLabelRenderer();
        Field field = help.optionFields.get(0);
        String[][] row = renderer.render(field.getAnnotation(Option.class), field, parameterRenderer);
        assertEquals(2, row.length);
        assertArrayEquals(Arrays.toString(row[0]), new String[]{"*", "-b", ",", "-a, --alpha=<otherField>", "other"}, row[0]);
        assertArrayEquals(Arrays.toString(row[1]), new String[]{"", "", "", "", "Default: abc"}, row[1]);
    }

    @Test
    public void testDefaultOptionRenderer_rendersSpecifiedMarkerForRequiredOptionsWithoutDefault() {
        @Command(requiredOptionMarker = '*', showDefaultValues = false)
        class Example {
            @Option(names = {"-b", "-a", "--alpha"}, required = true, description = "other") String otherField ="abc";
        }
        Help help = new Help(new Example());
        Help.IOptionRenderer renderer = help.createDefaultOptionRenderer();
        Help.IValueLabelRenderer parameterRenderer = help.createDefaultValueLabelRenderer();
        Field field = help.optionFields.get(0);
        String[][] row = renderer.render(field.getAnnotation(Option.class), field, parameterRenderer);
        assertEquals(1, row.length);
        assertArrayEquals(Arrays.toString(row[0]), new String[]{"*", "-b", ",", "-a, --alpha=<otherField>", "other"}, row[0]);
    }

    @Test
    public void testDefaultOptionRenderer_rendersSpacePrefixByDefaultForRequiredOptionsWithoutDefaultValue() {
        //@Command(showDefaultValues = false) set programmatically
        class Example {
            @Option(names = {"-b", "-a", "--alpha"}, required = true, description = "other") String otherField;
        }
        Help help = new Help(new Example());
        help.showDefaultValues = false;
        Help.IOptionRenderer renderer = help.createDefaultOptionRenderer();
        Help.IValueLabelRenderer parameterRenderer = help.createDefaultValueLabelRenderer();
        Field field = help.optionFields.get(0);
        String[][] row = renderer.render(field.getAnnotation(Option.class), field, parameterRenderer);
        assertEquals(1, row.length);
        assertArrayEquals(Arrays.toString(row[0]), new String[]{" ", "-b", ",", "-a, --alpha=<otherField>", "other"}, row[0]);
    }

    @Test
    public void testDefaultOptionRenderer_rendersSpacePrefixByDefaultForRequiredOptionsWithDefaultValue() {
        class Example {
            @Option(names = {"-b", "-a", "--alpha"}, required = true, description = "other") String otherField;
        }
        Help help = new Help(new Example());
        Help.IOptionRenderer renderer = help.createDefaultOptionRenderer();
        Help.IValueLabelRenderer parameterRenderer = help.createDefaultValueLabelRenderer();
        Field field = help.optionFields.get(0);
        String[][] row = renderer.render(field.getAnnotation(Option.class), field, parameterRenderer);
        assertEquals(2, row.length);
        assertArrayEquals(Arrays.toString(row[0]), new String[]{" ", "-b", ",", "-a, --alpha=<otherField>", "other"}, row[0]);
        assertArrayEquals(Arrays.toString(row[1]), new String[]{"",    "", "",  "", "Default: null"}, row[1]);
    }

    @Test
    public void testDefaultParameterRenderer_rendersSpacePrefixByDefaultForParametersWithPositiveArity() {
        class Required {
            @Parameters(description = "required") String required;
        }
        Help help = new Help(new Required());
        Help.IParameterRenderer renderer = help.createDefaultParameterRenderer();
        Help.IValueLabelRenderer parameterRenderer = Help.createMinimalValueLabelRenderer();
        Field field = help.positionalParametersFields.get(0);
        String[][] row1 = renderer.render(field.getAnnotation(Parameters.class), field, parameterRenderer);
        assertEquals(1, row1.length);
        assertArrayEquals(Arrays.toString(row1[0]), new String[]{" ", "", "", "required", "required"}, row1[0]);
    }

    @Test
    public void testDefaultParameterRenderer_rendersSpecifiedMarkerForParametersWithPositiveArity() {
        @Command(requiredOptionMarker = '*')
        class Required {
            @Parameters(description = "required") String required;
        }
        Help help = new Help(new Required());
        Help.IParameterRenderer renderer = help.createDefaultParameterRenderer();
        Help.IValueLabelRenderer parameterRenderer = Help.createMinimalValueLabelRenderer();
        Field field = help.positionalParametersFields.get(0);
        String[][] row1 = renderer.render(field.getAnnotation(Parameters.class), field, parameterRenderer);
        assertEquals(1, row1.length);
        assertArrayEquals(Arrays.toString(row1[0]), new String[]{"*", "", "", "required", "required"}, row1[0]);
    }

    @Test
    public void testDefaultParameterRenderer_rendersSpacePrefixForParametersWithZeroArity() {
        @Command(requiredOptionMarker = '*')
        class Optional {
            @Parameters(arity = "0..1", description = "optional") String optional;
        }
        Help help = new Help(new Optional());
        Help.IParameterRenderer renderer = help.createDefaultParameterRenderer();
        Help.IValueLabelRenderer parameterRenderer = Help.createMinimalValueLabelRenderer();
        Field field = help.positionalParametersFields.get(0);
        String[][] row1 = renderer.render(field.getAnnotation(Parameters.class), field, parameterRenderer);
        assertEquals(1, row1.length);
        assertArrayEquals(Arrays.toString(row1[0]), new String[]{"", "", "", "optional", "optional"}, row1[0]);
    }

    @Test
    public void testDefaultOptionRenderer_rendersCommaOnlyIfBothShortAndLongOptionNamesExist() {
        class Example {
            @Option(names = {"-v"}, description = "shortBool") boolean shortBoolean;
            @Option(names = {"--verbose"}, description = "longBool") boolean longBoolean;
            @Option(names = {"-x", "--xeno"}, description = "combiBool") boolean combiBoolean;
            @Option(names = {"-s"}, description = "shortOnly") String shortOnlyField;
            @Option(names = {"--long"}, description = "longOnly") String longOnlyField;
            @Option(names = {"-b", "--beta"}, description = "combi") String combiField;
        }
        Help help = new Help(new Example());
        help.showDefaultValues = false; // omit default values from description column
        Help.IOptionRenderer renderer = help.createDefaultOptionRenderer();
        Help.IValueLabelRenderer parameterRenderer = help.createDefaultValueLabelRenderer();

        String[][] expected = new String[][] {
                {"", "-v", "",  "", "shortBool"},
                {"", "",   "",  "--verbose", "longBool"},
                {"", "-x", ",", "--xeno", "combiBool"},
                {"", "-s", "",  "=<shortOnlyField>", "shortOnly"},
                {"", "",   "",  "--long=<longOnlyField>", "longOnly"},
                {"", "-b", ",", "--beta=<combiField>", "combi"},
        };
        int i = -1;
        for (Field field : help.optionFields) {
            String[][] row = renderer.render(field.getAnnotation(Option.class), field, parameterRenderer);
            assertEquals(1, row.length);
            assertArrayEquals(Arrays.toString(row[0]), expected[++i], row[0]);
        }
    }

    @Test
    public void testDefaultOptionRenderer_omitsDefaultValuesForBooleanFields() {
        class Example {
            @Option(names = {"-v"}, description = "shortBool") boolean shortBoolean;
            @Option(names = {"--verbose"}, description = "longBool") Boolean longBoolean;
            @Option(names = {"-s"}, description = "shortOnly") String shortOnlyField = "short";
            @Option(names = {"--long"}, description = "longOnly") String longOnlyField = "long";
            @Option(names = {"-b", "--beta"}, description = "combi") int combiField = 123;
        }
        Help help = new Help(new Example());
        Help.IOptionRenderer renderer = help.createDefaultOptionRenderer();
        Help.IValueLabelRenderer parameterRenderer = help.createDefaultValueLabelRenderer();

        String[][] expected = new String[][] {
                {"", "-v", "",  "", "shortBool"},
                {"", "",   "",  "--verbose", "longBool"},
                {"", "-s", "",  "=<shortOnlyField>", "shortOnly"},
                {"",   "", "",  "", "Default: short"},
                {"", "",   "",  "--long=<longOnlyField>", "longOnly"},
                {"", "",   "",  "", "Default: long"},
                {"", "-b", ",", "--beta=<combiField>", "combi"},
                {"", "",   "",  "", "Default: 123"},
        };
        int[] rowCount = {1, 1, 2, 2, 2};
        int i = -1;
        int rowIndex = 0;
        for (Field field : help.optionFields) {
            String[][] row = renderer.render(field.getAnnotation(Option.class), field, parameterRenderer);
            assertEquals(rowCount[++i], row.length);
            assertArrayEquals(Arrays.toString(row[0]), expected[rowIndex], row[0]);
            rowIndex += rowCount[i];
        }
    }

    @Test
    public void testCreateDefaultParameterRenderer_ReturnsDefaultParameterRenderer() {
        assertEquals(Help.DefaultValueLabelRenderer.class, new Help(new UsageDemo()).createDefaultValueLabelRenderer().getClass());
    }

    @Test
    public void testDefaultParameterRenderer_showsParamLabelIfPresentOrFieldNameOtherwise() {
        class Example {
            @Option(names = "--without" ) String longField;
            @Option(names = "--with", valueLabel = "LABEL") String otherField;
        }
        Help help = new Help(new Example());
        Help.IValueLabelRenderer equalSeparatedParameterRenderer = help.createDefaultValueLabelRenderer();
        help.separator = " ";
        Help.IValueLabelRenderer spaceSeparatedParameterRenderer = help.createDefaultValueLabelRenderer();

        String[] expected = new String[] {
                "<longField>",
                "LABEL",
        };
        int i = -1;
        for (Field field : help.optionFields) {
            i++;
            String withSpace = spaceSeparatedParameterRenderer.renderParameterLabel(field);
            assertEquals(withSpace, " " + expected[i], withSpace);
            String withEquals = equalSeparatedParameterRenderer.renderParameterLabel(field);
            assertEquals(withEquals, "=" + expected[i], withEquals);
        }
    }

    @Test
    public void testDefaultParameterRenderer_appliesToPositionalArgumentsIgnoresSeparator() {
        class WithLabel    { @Parameters(valueLabel = "POSITIONAL_ARGS") String positional; }
        class WithoutLabel { @Parameters()                               String positional; }

        Help withLabel = new Help(new WithLabel());
        Help.IValueLabelRenderer equals = withLabel.createDefaultValueLabelRenderer();
        withLabel.separator = "=";
        Help.IValueLabelRenderer spaced = withLabel.createDefaultValueLabelRenderer();

        String withSpace = spaced.renderParameterLabel(withLabel.positionalParametersFields.get(0));
        assertEquals(withSpace, "POSITIONAL_ARGS", withSpace);
        String withEquals = equals.renderParameterLabel(withLabel.positionalParametersFields.get(0));
        assertEquals(withEquals, "POSITIONAL_ARGS", withEquals);

        Help withoutLabel = new Help(new WithoutLabel());
        withSpace = spaced.renderParameterLabel(withoutLabel.positionalParametersFields.get(0));
        assertEquals(withSpace, "<positional>", withSpace);
        withEquals = equals.renderParameterLabel(withoutLabel.positionalParametersFields.get(0));
        assertEquals(withEquals, "<positional>", withEquals);
    }

    @Test
    public void testDefaultLayout_addsEachRowToTable() {
        final String[][] values = { {"a", "b", "c", "d" }, {"1", "2", "3", "4"} };
        final int[] count = {0};
        TextTable tt = new TextTable() {
            @Override public void addRowValues(String[] columnValues) {
                assertArrayEquals(values[count[0]], columnValues);
                count[0]++;
            }
        };
        Help.Layout layout = new Help.Layout(tt);
        layout.layout(null, values);
        assertEquals(2, count[0]);
    }

    @Test
    public void testAbreviatedSynopsis_withoutParameters() {
        @CommandLine.Command(abbreviateSynopsis = true)
        class App {
            @Option(names = {"--verbose", "-v"}) boolean verbose;
            @Option(names = {"--count", "-c"}) int count;
            @Option(names = {"--help", "-h"}, hidden = true) boolean helpRequested;
        }
        assertEquals("<main class> [OPTIONS]" + LINESEP, new Help(new App()).synopsis());
    }

    @Test
    public void testAbreviatedSynopsis_withParameters() {
        @CommandLine.Command(abbreviateSynopsis = true)
        class App {
            @Option(names = {"--verbose", "-v"}) boolean verbose;
            @Option(names = {"--count", "-c"}) int count;
            @Option(names = {"--help", "-h"}, hidden = true) boolean helpRequested;
            @Parameters File[] files;
        }
        assertEquals("<main class> [OPTIONS] [<files>...]" + LINESEP, new Help(new App()).synopsis());
    }

    @Test
    public void testSynopsis_optionalOptionArity1_n_withDefaultSeparator() {
        @Command() class App {
            @Option(names = {"--verbose", "-v"}) boolean verbose;
            @Option(names = {"--count", "-c"}, arity = "1..*") int count;
            @Option(names = {"--help", "-h"}, hidden = true) boolean helpRequested;
        }
        assertEquals("<main class> [-v] [-c=<count> [<count>...]]" + LINESEP, new Help(new App()).synopsis());
    }

    @Test
    public void testSynopsis_optionalOptionArity0_1_withSpaceSeparator() {
        @CommandLine.Command(separator = " ") class App {
            @Option(names = {"--verbose", "-v"}) boolean verbose;
            @Option(names = {"--count", "-c"}, arity = "0..1") int count;
            @Option(names = {"--help", "-h"}, hidden = true) boolean helpRequested;
        }
        assertEquals("<main class> [-v] [-c [<count>]]" + LINESEP, new Help(new App()).synopsis());
    }

    @Test
    public void testSynopsis_requiredOptionWithSeparator() {
        @Command() class App {
            @Option(names = {"--verbose", "-v"}) boolean verbose;
            @Option(names = {"--count", "-c"}, required = true) int count;
            @Option(names = {"--help", "-h"}, hidden = true) boolean helpRequested;
        }
        assertEquals("<main class> [-v] -c=<count>" + LINESEP, new Help(new App()).synopsis());
    }

    @Test
    public void testSynopsis_optionalOption_withSpaceSeparator() {
        @CommandLine.Command(separator = " ") class App {
            @Option(names = {"--verbose", "-v"}) boolean verbose;
            @Option(names = {"--count", "-c"}) int count;
            @Option(names = {"--help", "-h"}, hidden = true) boolean helpRequested;
        }
        assertEquals("<main class> [-v] [-c <count>]" + LINESEP, new Help(new App()).synopsis());
    }

    @Test
    public void testSynopsis_optionalOptionArity0_1__withSeparator() {
        class App {
            @Option(names = {"--verbose", "-v"}) boolean verbose;
            @Option(names = {"--count", "-c"}, arity = "0..1") int count;
            @Option(names = {"--help", "-h"}, hidden = true) boolean helpRequested;
        }
        assertEquals("<main class> [-v] [-c[=<count>]]" + LINESEP, new Help(new App()).synopsis());
    }

    @Test
    public void testSynopsis_optionalOptionArity0_n__withSeparator() {
        @CommandLine.Command(separator = "=") class App {
            @Option(names = {"--verbose", "-v"}) boolean verbose;
            @Option(names = {"--count", "-c"}, arity = "0..*") int count;
            @Option(names = {"--help", "-h"}, hidden = true) boolean helpRequested;
        }
        assertEquals("<main class> [-v] [-c[=<count>...]]" + LINESEP, new Help(new App()).synopsis());
    }

    @Test
    public void testSynopsis_optionalOptionArity1_n__withSeparator() {
        @CommandLine.Command(separator = "=") class App {
            @Option(names = {"--verbose", "-v"}) boolean verbose;
            @Option(names = {"--count", "-c"}, arity = "1..*") int count;
            @Option(names = {"--help", "-h"}, hidden = true) boolean helpRequested;
        }
        assertEquals("<main class> [-v] [-c=<count> [<count>...]]" + LINESEP, new Help(new App()).synopsis());
    }

    @Test
    public void testSynopsis_withSeparator_withParameters() {
        @CommandLine.Command(separator = ":") class App {
            @Option(names = {"--verbose", "-v"}) boolean verbose;
            @Option(names = {"--count", "-c"}) int count;
            @Option(names = {"--help", "-h"}, hidden = true) boolean helpRequested;
            @Parameters File[] files;
        }
        assertEquals("<main class> [-v] [-c:<count>] [<files>...]" + LINESEP, new Help(new App()).synopsis());
    }

    @Test
    public void testSynopsis_withSeparator_withLabeledParameters() {
        @Command(separator = "=") class App {
            @Option(names = {"--verbose", "-v"}) boolean verbose;
            @Option(names = {"--count", "-c"}) int count;
            @Option(names = {"--help", "-h"}, hidden = true) boolean helpRequested;
            @Parameters(valueLabel = "FILE") File[] files;
        }
        assertEquals("<main class> [-v] [-c=<count>] [FILE...]" + LINESEP, new Help(new App()).synopsis());
    }

    @Test
    public void testSynopsis_withSeparator_withLabeledRequiredParameters() {
        @CommandLine.Command(separator = "=") class App {
            @Option(names = {"--verbose", "-v"}) boolean verbose;
            @Option(names = {"--count", "-c"}) int count;
            @Option(names = {"--help", "-h"}, hidden = true) boolean helpRequested;
            @Parameters(valueLabel = "FILE", arity = "1..*") File[] files;
        }
        assertEquals("<main class> [-v] [-c=<count>] FILE [FILE...]" + LINESEP, new Help(new App()).synopsis());
    }

    @Test
    public void testSynopsis_clustersBooleanOptions() {
        @Command(separator = "=") class App {
            @Option(names = {"--verbose", "-v"}) boolean verbose;
            @Option(names = {"--aaaa", "-a"}) boolean aBoolean;
            @Option(names = {"--xxxx", "-x"}) Boolean xBoolean;
            @Option(names = {"--count", "-c"}, valueLabel = "COUNT") int count;
        }
        assertEquals("<main class> [-avx] [-c=COUNT]" + LINESEP, new Help(new App()).synopsis());
    }

    @Test
    public void testSynopsis_clustersRequiredBooleanOptions() {
        @CommandLine.Command(separator = "=") class App {
            @Option(names = {"--verbose", "-v"}, required = true) boolean verbose;
            @Option(names = {"--aaaa", "-a"}, required = true) boolean aBoolean;
            @Option(names = {"--xxxx", "-x"}, required = true) Boolean xBoolean;
            @Option(names = {"--count", "-c"}, valueLabel = "COUNT") int count;
        }
        assertEquals("<main class> -avx [-c=COUNT]" + LINESEP, new Help(new App()).synopsis());
    }

    @Test
    public void testSynopsis_clustersRequiredBooleanOptionsSeparately() {
        @CommandLine.Command(separator = "=") class App {
            @Option(names = {"--verbose", "-v"}) boolean verbose;
            @Option(names = {"--aaaa", "-a"}) boolean aBoolean;
            @Option(names = {"--xxxx", "-x"}) Boolean xBoolean;
            @Option(names = {"--Verbose", "-V"}, required = true) boolean requiredVerbose;
            @Option(names = {"--Aaaa", "-A"}, required = true) boolean requiredABoolean;
            @Option(names = {"--Xxxx", "-X"}, required = true) Boolean requiredXBoolean;
            @Option(names = {"--count", "-c"}, valueLabel = "COUNT") int count;
        }
        assertEquals("<main class> -AVX [-avx] [-c=COUNT]" + LINESEP, new Help(new App()).synopsis());
    }

    @Test
    public void testLongMultiLineSynopsisIndented() {
        @Command(name = "<best-app-ever>")
        class App {
            @Option(names = "--long-option-name", valueLabel = "<long-option-value>") int a;
            @Option(names = "--another-long-option-name", valueLabel = "<another-long-option-value>") int b;
            @Option(names = "--third-long-option-name", valueLabel = "<third-long-option-value>") int c;
            @Option(names = "--fourth-long-option-name", valueLabel = "<fourth-long-option-value>") int d;
        }
        assertEquals(String.format(
                "<best-app-ever> [--another-long-option-name=<another-long-option-value>]%n" +
                "                [--fourth-long-option-name=<fourth-long-option-value>]%n" +
                "                [--long-option-name=<long-option-value>]%n" +
                "                [--third-long-option-name=<third-long-option-value>]%n"),
                new Help(new App()).synopsis());
    }

    @Test
    public void testLongMultiLineSynopsisWithAtMarkIndented() {
        @Command(name = "<best-app-ever>")
        class App {
            @Option(names = "--long-option@-name", valueLabel = "<long-option-valu@@e>") int a;
            @Option(names = "--another-long-option-name", valueLabel = "^[<another-long-option-value>]") int b;
            @Option(names = "--third-long-option-name", valueLabel = "<third-long-option-value>") int c;
            @Option(names = "--fourth-long-option-name", valueLabel = "<fourth-long-option-value>") int d;
        }
        assertEquals(String.format(
                "<best-app-ever> [--another-long-option-name=^[<another-long-option-value>]]%n" +
                "                [--fourth-long-option-name=<fourth-long-option-value>]%n" +
                "                [--long-option@-name=<long-option-valu@@e>]%n" +
                "                [--third-long-option-name=<third-long-option-value>]%n"),
                new Help(new App()).synopsis());
    }

    @Test
    public void testCustomSynopsis() {
        @Command(customSynopsis = {
                "<the-app> --number=NUMBER --other-option=<aargh>",
                "          --more=OTHER --and-other-option=<aargh>",
                "<the-app> --number=NUMBER --and-other-option=<aargh>",
        })
        class App {@Option(names = "--ignored") boolean ignored;}
        assertEquals(String.format(
                "<the-app> --number=NUMBER --other-option=<aargh>%n" +
                "          --more=OTHER --and-other-option=<aargh>%n" +
                "<the-app> --number=NUMBER --and-other-option=<aargh>%n"),
                new Help(new App()).synopsis());
    }
    @Test
    public void testTextTable() {
        TextTable table = new TextTable();
        table.addRowValues("", "-v", ",", "--verbose", "show what you're doing while you are doing it");
        table.addRowValues("", "-p", null, null, "the quick brown fox jumped over the lazy dog. The quick brown fox jumped over the lazy dog.");
        assertEquals(String.format(
                "  -v, --verbose               show what you're doing while you are doing it%n" +
                "  -p                          the quick brown fox jumped over the lazy dog. The%n" +
                "                                quick brown fox jumped over the lazy dog.%n"
                ,""), table.toString(new StringBuilder()).toString());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testTextTableAddsNewRowWhenTooManyValuesSpecified() {
        TextTable table = new TextTable();
        table.addRowValues("", "-c", ",", "--create", "description", "INVALID", "Row 3");
//        assertEquals(String.format("" +
//                        "  -c, --create                description                                       %n" +
//                        "                                INVALID                                         %n" +
//                        "                                Row 3                                           %n"
//                ,""), table.toString(new StringBuilder()).toString());
    }

    @Test
    public void testTextTableAddsNewRowWhenAnyColumnTooLong() {
        TextTable table = new TextTable();
        table.addRowValues("*", "-c", ",",
                "--create, --create2, --create3, --create4, --create5, --create6, --create7, --create8",
                "description");
        assertEquals(String.format("" +
                        "* -c, --create, --create2, --create3, --create4, --create5, --create6,%n" +
                        "        --create7, --create8%n" +
                        "                              description%n"
                ,""), table.toString(new StringBuilder()).toString());

        table = new TextTable();
        table.addRowValues("", "-c", ",",
                "--create, --create2, --create3, --create4, --create5, --create6, --createAA7, --create8",
                "description");
        assertEquals(String.format("" +
                        "  -c, --create, --create2, --create3, --create4, --create5, --create6,%n" +
                        "        --createAA7, --create8%n" +
                        "                              description%n"
                ,""), table.toString(new StringBuilder()).toString());
    }

    @Test
    public void testCatUsageFormat() {
        @Command(name = "cat",
                abbreviateSynopsis = true,
                description = "Concatenate FILE(s), or standard input, to standard output.",
                footer = "Copyright(c) 2017")
        class Cat {
            @Parameters(valueLabel = "FILE", hidden = true, description = "Files whose contents to display") List<File> files;
            @Option(names = "--help",    help = true,     description = "display this help and exit") boolean help;
            @Option(names = "--version", help = true,     description = "output version information and exit") boolean version;
            @Option(names = "-u",                         description = "(ignored)") boolean u;
            @Option(names = "-t",                         description = "equivalent to -vT") boolean t;
            @Option(names = "-e",                         description = "equivalent to -vET") boolean e;
            @Option(names = {"-A", "--show-all"},         description = "equivalent to -vET") boolean showAll;
            @Option(names = {"-s", "--squeeze-blank"},    description = "suppress repeated empty output lines") boolean squeeze;
            @Option(names = {"-v", "--show-nonprinting"}, description = "use ^ and M- notation, except for LDF and TAB") boolean v;
            @Option(names = {"-b", "--number-nonblank"},  description = "number nonempty output lines, overrides -n") boolean b;
            @Option(names = {"-T", "--show-tabs"},        description = "display TAB characters as ^I") boolean T;
            @Option(names = {"-E", "--show-ends"},        description = "display $ at end of each line") boolean E;
            @Option(names = {"-n", "--number"},           description = "number all output lines") boolean n;
        }
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        CommandLine.usage(new Cat(), new PrintStream(baos));
        String expected = String.format(
                "Usage: cat [OPTIONS] [FILE...]%n" +
                        "Concatenate FILE(s), or standard input, to standard output.%n" +
                        "  -A, --show-all              equivalent to -vET%n" +
                        "  -b, --number-nonblank       number nonempty output lines, overrides -n%n" +
                        "  -e                          equivalent to -vET%n" +
                        "  -E, --show-ends             display $ at end of each line%n" +
                        "  -n, --number                number all output lines%n" +
                        "  -s, --squeeze-blank         suppress repeated empty output lines%n" +
                        "  -t                          equivalent to -vT%n" +
                        "  -T, --show-tabs             display TAB characters as ^I%n" +
                        "  -u                          (ignored)%n" +
                        "  -v, --show-nonprinting      use ^ and M- notation, except for LDF and TAB%n" +
                        "      --help                  display this help and exit%n" +
                        "      --version               output version information and exit%n" +
                        "Copyright(c) 2017%n", "");
        assertEquals(expected, baos.toString());
    }

    @Test
    public void testZipUsageFormat() {
        @CommandLine.Command(description = {
                "Copyright (c) 1990-2008 Info-ZIP - Type 'zip \"-L\"' for software license.",
                "Zip 3.0 (July 5th 2008). Command:",
                "zip [-options] [-b path] [-t mmddyyyy] [-n suffixes] [zipfile list] [-xi list]",
                "  The default action is to add or replace zipfile entries from list, which",
                "  can include the special name - to compress standard input.",
                "  If zipfile and list are omitted, zip compresses stdin to stdout."}
        )
        class Zip {
            @Option(names = "-f", description = "freshen: only changed files") boolean freshen;
            @Option(names = "-u", description = "update: only changed or new files") boolean update;
            @Option(names = "-d", description = "delete entries in zipfile") boolean delete;
            @Option(names = "-m", description = "move into zipfile (delete OS files)") boolean move;
            @Option(names = "-r", description = "recurse into directories") boolean recurse;
            @Option(names = "-j", description = "junk (don't record) directory names") boolean junk;
            @Option(names = "-0", description = "store only") boolean store;
            @Option(names = "-l", description = "convert LF to CR LF (-ll CR LF to LF)") boolean lf2crlf;
            @Option(names = "-1", description = "compress faster") boolean faster;
            @Option(names = "-9", description = "compress better") boolean better;
            @Option(names = "-q", description = "quiet operation") boolean quiet;
            @Option(names = "-v", description = "verbose operation/print version info") boolean verbose;
            @Option(names = "-c", description = "add one-line comments") boolean comments;
            @Option(names = "-z", description = "add zipfile comment") boolean zipComment;
            @Option(names = "-@", description = "read names from stdin") boolean readFileList;
            @Option(names = "-o", description = "make zipfile as old as latest entry") boolean old;
            @Option(names = "-x", description = "exclude the following names") boolean exclude;
            @Option(names = "-i", description = "include only the following names") boolean include;
            @Option(names = "-F", description = "fix zipfile (-FF try harder)") boolean fix;
            @Option(names = "-D", description = "do not add directory entries") boolean directories;
            @Option(names = "-A", description = "adjust self-extracting exe") boolean adjust;
            @Option(names = "-J", description = "junk zipfile prefix (unzipsfx)") boolean junkPrefix;
            @Option(names = "-T", description = "test zipfile integrity") boolean test;
            @Option(names = "-X", description = "eXclude eXtra file attributes") boolean excludeAttribs;
            @Option(names = "-y", description = "store symbolic links as the link instead of the referenced file") boolean symbolic;
            @Option(names = "-e", description = "encrypt") boolean encrypt;
            @Option(names = "-n", description = "don't compress these suffixes") boolean dontCompress;
            @Option(names = "-h2", description = "show more help") boolean moreHelp;
        }

        class TwoOptionsPerRowLayout extends Help.Layout { // define a custom layout
            Point previous = new Point(0, 0);

            public TwoOptionsPerRowLayout(TextTable textTable,
                                          Help.IOptionRenderer optionRenderer,
                                          Help.IParameterRenderer parameterRenderer) {
                super(textTable, optionRenderer, parameterRenderer);
            }

            @Override public void layout(Field field, String[][] values) {
                String[] columnValues = values[0]; // we know renderer creates a single row with two values

                // We want to show two options on one row, next to each other,
                // unless the first option spanned multiple columns (in which case there are not enough columns left)
                int col = previous.x + 1;
                if (col == 1 || col + columnValues.length > table.columns.length) { // if true, write into next row

                    // table also adds an empty row if a text value spanned multiple columns
                    if (table.rowCount() == 0 || table.rowCount() == previous.y + 1) { // avoid adding 2 empty rows
                        table.addEmptyRow(); // create the slots to write the text values into
                    }
                    col = 0; // we are starting a new row, reset the column to write into
                }
                for (int i = 0; i < columnValues.length; i++) {
                    // always write to the last row, column depends on what happened previously
                    previous = table.putValue(table.rowCount() - 1, col + i, columnValues[i]);
                }
            }
        }
        TextTable textTable = new TextTable(new Column[] {
                new Column(5, 2, TRUNCATE), // values should fit
                new Column(30, 2, SPAN), // overflow into adjacent columns
                new Column(4,  1, TRUNCATE), // values should fit again
                new Column(39, 2, WRAP)
        });
        TwoOptionsPerRowLayout layout = new TwoOptionsPerRowLayout(textTable, Help.createMinimalOptionRenderer(),
                Help.createMinimalParameterRenderer());

        Help help = new Help(new Zip());
        StringBuilder sb = new StringBuilder();
        sb.append(help.description()); // show the first 6 lines, including copyright, description and usage

        // Note that we don't sort the options, so they appear in the order the fields are declared in the Zip class.
        layout.addOptions(help.optionFields, help.parameterLabelRenderer);
        sb.append(layout); // finally, copy the options details help text into the StringBuilder

        String expected  = String.format("" +
                "Copyright (c) 1990-2008 Info-ZIP - Type 'zip \"-L\"' for software license.%n" +
                "Zip 3.0 (July 5th 2008). Command:%n" +
                "zip [-options] [-b path] [-t mmddyyyy] [-n suffixes] [zipfile list] [-xi list]%n" +
                "  The default action is to add or replace zipfile entries from list, which%n" +
                "  can include the special name - to compress standard input.%n" +
                "  If zipfile and list are omitted, zip compresses stdin to stdout.%n" +
                "  -f   freshen: only changed files  -u   update: only changed or new files%n" +
                "  -d   delete entries in zipfile    -m   move into zipfile (delete OS files)%n" +
                "  -r   recurse into directories     -j   junk (don't record) directory names%n" +
                "  -0   store only                   -l   convert LF to CR LF (-ll CR LF to LF)%n" +
                "  -1   compress faster              -9   compress better%n" +
                "  -q   quiet operation              -v   verbose operation/print version info%n" +
                "  -c   add one-line comments        -z   add zipfile comment%n" +
                "  -@   read names from stdin        -o   make zipfile as old as latest entry%n" +
                "  -x   exclude the following names  -i   include only the following names%n" +
                "  -F   fix zipfile (-FF try harder) -D   do not add directory entries%n" +
                "  -A   adjust self-extracting exe   -J   junk zipfile prefix (unzipsfx)%n" +
                "  -T   test zipfile integrity       -X   eXclude eXtra file attributes%n" +
                "  -y   store symbolic links as the link instead of the referenced file%n" +
                "  -e   encrypt                      -n   don't compress these suffixes%n" +
                "  -h2  show more help%n");
        assertEquals(expected, sb.toString());
    }

    /** for Netstat test */
    private enum Protocol {IP, IPv6, ICMP, ICMPv6, TCP, TCPv6, UDP, UDPv6}
    @Test
    public void testNetstatUsageFormat() {
        @CommandLine.Command(name = "NETSTAT",
                separator = " ",
                abbreviateSynopsis = true,
                header = "Displays protocol statistics and current TCP/IP network connections.\n")
        class Netstat {
            @Option(names="-a", description="Displays all connections and listening ports.")
            boolean displayAll;
            @Option(names="-b", description="Displays the executable involved in creating each connection or "
                    + "listening port. In some cases well-known executables host "
                    + "multiple independent components, and in these cases the "
                    + "sequence of components involved in creating the connection "
                    + "or listening port is displayed. In this case the executable "
                    + "name is in [] at the bottom, on top is the component it called, "
                    + "and so forth until TCP/IP was reached. Note that this option "
                    + "can be time-consuming and will fail unless you have sufficient "
                    + "permissions.")
            boolean displayExecutable;
            @Option(names="-e", description="Displays Ethernet statistics. This may be combined with the -s option.")
            boolean displayEthernetStats;
            @Option(names="-f", description="Displays Fully Qualified Domain Names (FQDN) for foreign addresses.")
            boolean displayFQCN;
            @Option(names="-n", description="Displays addresses and port numbers in numerical form.")
            boolean displayNumerical;
            @Option(names="-o", description="Displays the owning process ID associated with each connection.")
            boolean displayOwningProcess;
            @Option(names="-p", valueLabel = "proto",
                    description="Shows connections for the protocol specified by proto; proto "
                    + "may be any of: TCP, UDP, TCPv6, or UDPv6.  If used with the -s "
                    + "option to display per-protocol statistics, proto may be any of: "
                    + "IP, IPv6, ICMP, ICMPv6, TCP, TCPv6, UDP, or UDPv6.")
            Protocol proto;
            @Option(names="-q", description="Displays all connections, listening ports, and bound "
                    + "nonlistening TCP ports. Bound nonlistening ports may or may not "
                    + "be associated with an active connection.")
            boolean query;
            @Option(names="-r", description="Displays the routing table.")
            boolean displayRoutingTable;
            @Option(names="-s", description="Displays per-protocol statistics.  By default, statistics are "
                    + "shown for IP, IPv6, ICMP, ICMPv6, TCP, TCPv6, UDP, and UDPv6; "
                    + "the -p option may be used to specify a subset of the default.")
            boolean displayStatistics;
            @Option(names="-t", description="Displays the current connection offload state.")
            boolean displayOffloadState;
            @Option(names="-x", description="Displays NetworkDirect connections, listeners, and shared endpoints.")
            boolean displayNetDirect;
            @Option(names="-y", description="Displays the TCP connection template for all connections. "
                    + "Cannot be combined with the other options.")
            boolean displayTcpConnectionTemplate;
            @Parameters(arity = "0..1", valueLabel = "interval", description = ""
                    + "Redisplays selected statistics, pausing interval seconds "
                    + "between each display.  Press CTRL+C to stop redisplaying "
                    + "statistics.  If omitted, netstat will print the current "
                    + "configuration information once.")
            int interval;
        }
        StringBuilder sb = new StringBuilder();
        Help help = new Help(new Netstat());
        sb.append(help.header()).append(help.detailedSynopsis(null, false));
        sb.append(System.getProperty("line.separator"));

        TextTable textTable = new TextTable(
                new Column(15, 2, TRUNCATE),
                new Column(65, 1, WRAP));
        textTable.indentWrappedLines = 0;
        Help.Layout layout = new Help.Layout(textTable, Help.createMinimalOptionRenderer(), Help.createMinimalParameterRenderer());
        layout.addOptions(help.optionFields, help.parameterLabelRenderer);
        layout.addPositionalParameters(help.positionalParametersFields, Help.createMinimalValueLabelRenderer());
        sb.append(layout);

        String expected = String.format("" +
                "Displays protocol statistics and current TCP/IP network connections.\n" +
                "%n" +
                "NETSTAT [-a] [-b] [-e] [-f] [-n] [-o] [-p proto] [-q] [-r] [-s] [-t] [-x] [-y]%n" +
                "        [interval]%n" +
                // FIXME needs Show multiple detailed usage header lines for mutually exclusive options #46
                // "NETSTAT [-a] [-b] [-e] [-f] [-n] [-o] [-p proto] [-q] [-r] [-s] [-t] [-x] [interval]%n" +
                // "NETSTAT [-y] [interval]%n" +
                "%n" +
                "  -a            Displays all connections and listening ports.%n" +
                "  -b            Displays the executable involved in creating each connection or%n" +
                "                listening port. In some cases well-known executables host%n" +
                "                multiple independent components, and in these cases the%n" +
                "                sequence of components involved in creating the connection or%n" +
                "                listening port is displayed. In this case the executable name%n" +
                "                is in [] at the bottom, on top is the component it called, and%n" +
                "                so forth until TCP/IP was reached. Note that this option can be%n" +
                "                time-consuming and will fail unless you have sufficient%n" +
                "                permissions.%n" +
                "  -e            Displays Ethernet statistics. This may be combined with the -s%n" +
                "                option.%n" +
                "  -f            Displays Fully Qualified Domain Names (FQDN) for foreign%n" +
                "                addresses.%n" +
                "  -n            Displays addresses and port numbers in numerical form.%n" +
                "  -o            Displays the owning process ID associated with each connection.%n" +
                "  -p proto      Shows connections for the protocol specified by proto; proto%n" +
                "                may be any of: TCP, UDP, TCPv6, or UDPv6.  If used with the -s%n" +
                "                option to display per-protocol statistics, proto may be any of:%n" +
                "                IP, IPv6, ICMP, ICMPv6, TCP, TCPv6, UDP, or UDPv6.%n" +
                "  -q            Displays all connections, listening ports, and bound%n" +
                "                nonlistening TCP ports. Bound nonlistening ports may or may not%n" +
                "                be associated with an active connection.%n" +
                "  -r            Displays the routing table.%n" +
                "  -s            Displays per-protocol statistics.  By default, statistics are%n" +
                "                shown for IP, IPv6, ICMP, ICMPv6, TCP, TCPv6, UDP, and UDPv6;%n" +
                "                the -p option may be used to specify a subset of the default.%n" +
                "  -t            Displays the current connection offload state.%n" +
                "  -x            Displays NetworkDirect connections, listeners, and shared%n" +
                "                endpoints.%n" +
                "  -y            Displays the TCP connection template for all connections.%n" +
                "                Cannot be combined with the other options.%n" +
                "  interval      Redisplays selected statistics, pausing interval seconds%n" +
                "                between each display.  Press CTRL+C to stop redisplaying%n" +
                "                statistics.  If omitted, netstat will print the current%n" +
                "                configuration information once.%n"
        , "");
        assertEquals(expected, sb.toString());
    }

    @Test
    public void testUsageIndexedPositionalParameters() throws UnsupportedEncodingException {
        @Command(showDefaultValues = false)
        class App {
            @Parameters(index = "0", description = "source host") InetAddress host1;
            @Parameters(index = "1", description = "source port") int port1;
            @Parameters(index = "2", description = "destination host") InetAddress host2;
            @Parameters(index = "3..4", arity = "1..2", description = "destination port range") int[] port2range;
            @Parameters(index = "4..*", description = "files to transfer") String[] files;
            @Parameters(hidden = true, synopsis = false) String[] all;
        }
        String actual = usageString(new App());
        String expected = String.format(
                "Usage: <main class> <host1> <port1> <host2> <port2range> [<port2range>] [<files>...]%n" +
                "      host1                   source host%n" +
                "      port1                   source port%n" +
                "      host2                   destination host%n" +
                "      port2range              destination port range%n" +
                "      files                   files to transfer%n"
        );
        assertEquals(expected, actual);
    }

    static class UsageDemo {
        @Option(names = "-a", description = "boolean option with short name only")
        boolean a;

        @Option(names = "-b", valueLabel = "INT", description = "short option with a parameter")
        int b;

        @Option(names = {"-c", "--c-option"}, description = "boolean option with short and long name")
        boolean c;

        @Option(names = {"-d", "--d-option"}, valueLabel = "FILE", description = "option with parameter and short and long name")
        File d;

        @Option(names = "--e-option", description = "boolean option with only a long name")
        boolean e;

        @Option(names = "--f-option", valueLabel = "STRING", description = "option with parameter and only a long name")
        String f;

        @Option(names = {"-g", "--g-option-with-a-name-so-long-that-it-runs-into-the-descriptions-column"}, description = "boolean option with short and long name")
        boolean g;

        @Parameters(index = "0", valueLabel = "0BLAH", description = "first parameter")
        String param0;

        @Parameters(index = "1", valueLabel = "1PARAMETER-with-a-name-so-long-that-it-runs-into-the-descriptions-column", description = "2nd parameter")
        String param1;

        @Parameters(index = "2..*", valueLabel = "remaining", description = "remaining parameters")
        String param2_n;

        @Parameters(index = "*", valueLabel = "all", description = "all parameters")
        String param_n;
    }
}
