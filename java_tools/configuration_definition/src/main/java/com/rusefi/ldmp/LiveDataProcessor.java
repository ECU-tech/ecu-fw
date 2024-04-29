package com.rusefi.ldmp;

import com.devexperts.logging.Logging;
import com.rusefi.EnumToString;
import com.rusefi.InvokeReader;
import com.rusefi.ReaderStateImpl;
import com.rusefi.RusefiParseErrorStrategy;
import com.rusefi.newparse.ParseState;
import com.rusefi.newparse.outputs.CStructWriter;
import com.rusefi.newparse.parsing.Definition;
import com.rusefi.output.*;
import com.rusefi.util.LazyFile;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class LiveDataProcessor {
    private final static Logging log = Logging.getLogging(LiveDataProcessor.class);

    private final static String header = "// generated by gen_live_documentation.sh / LiveDataProcessor.java\n";

    private final static String enumContentFileName = "generated/live_data_ids.h";

    private final static String tsOutputsDestination = "generated/";

    private final StringBuilder enumContent = new StringBuilder(header +
            "#pragma once\n" +
            "\n" +
            "typedef enum {\n");

    private final StringBuilder baseAddressCHeader = new StringBuilder();

    private final StringBuilder fancyNewStuff = new StringBuilder();

    private final StringBuilder fancyNewMenu = new StringBuilder();

    private final StringBuilder fragmentsContent = new StringBuilder(header);

    private final String extraPrepend = System.getProperty("LiveDataProcessor.extra_prepend");

    public static void main(String[] args) throws IOException {
        if (args.length != 1) {
            System.err.println("One parameter expected: name of live data yaml input file");
            System.exit(-1);
        }

        // ensure outputs directory exists - it's gitignored
        Files.createDirectories(Paths.get(tsOutputsDestination));

        String yamlFileName = args[0];
        Yaml yaml = new Yaml();
        Map<String, Object> data = yaml.load(new FileReader(yamlFileName));

        LiveDataProcessor liveDataProcessor = new LiveDataProcessor();

        int sensorTsPosition = liveDataProcessor.handleYaml(data);
        liveDataProcessor.writeFiles();

        log.info("TS_TOTAL_OUTPUT_SIZE=" + sensorTsPosition);
        try (FileWriter fw = new FileWriter("generated/total_live_data_generated.h")) {
            fw.write(header);
            fw.write("#define TS_TOTAL_OUTPUT_SIZE " + sensorTsPosition);
        }

        try (FileWriter fw = new FileWriter(tsOutputsDestination + "fancy_content.ini")) {
            fw.write(liveDataProcessor.fancyNewStuff.toString());
        }

        try (FileWriter fw = new FileWriter(tsOutputsDestination + "fancy_menu.ini")) {
            fw.write(liveDataProcessor.fancyNewMenu.toString());
        }
    }

    interface EntryHandler {
        void onEntry(String name, String javaName, String folder, String prepend, boolean withCDefines, String[] outputNames, String constexpr, String conditional, Boolean isPtr) throws IOException;
    }

    private int handleYaml(Map<String, Object> data) throws IOException {
        OutputsSectionConsumer outputsSections = new OutputsSectionConsumer(tsOutputsDestination + File.separator + "output_channels.ini");

        ConfigurationConsumer dataLogConsumer = new DataLogConsumer(tsOutputsDestination + File.separator + "data_logs.ini");

        SdCardFieldsContent sdCardFieldsConsumer = new SdCardFieldsContent();

        GetOutputValueConsumer outputValueConsumer = new GetOutputValueConsumer("generated/output_lookup_generated.cpp");

        // OutputChannelWriter outputChannelWriter = new OutputChannelWriter(
        //     tsOutputsDestination + File.separator + "/output_channels.ini",
        //     tsOutputsDestination + File.separator + "/data_logs.ini"
        // );

        // SdLogWriter sdLogWriter = new SdLogWriter("console/binary_log/log_fields_generated.h");

        EntryHandler handler = (name, javaName, folder, prepend, withCDefines, outputNames, constexpr, conditional, isPtr) -> {
            int startingPosition = outputsSections.sensorTsPosition;
            log.info("Starting " + name + " at " + startingPosition + " with [" + conditional + "]");

            baseAddressCHeader.append("#define " + name.toUpperCase() + "_BASE_ADDRESS " + startingPosition + "\n");

            ReaderStateImpl state = new ReaderStateImpl();
            state.setDefinitionInputFile(folder + File.separator + name + ".txt");
            state.setWithC_Defines(withCDefines);

            state.addDestination(
                    outputsSections,
                    dataLogConsumer
            );
            FragmentDialogConsumer fragmentDialogConsumer = new FragmentDialogConsumer(name);
            state.addDestination(fragmentDialogConsumer);

            if (extraPrepend != null)
                state.addPrepend(extraPrepend);
            state.addPrepend(prepend);
            String cHeaderDestination = folder + File.separator + name + "_generated.h";

            int baseOffset = outputsSections.getBaseOffset();

           if (javaName != null) {
               state.addDestination(new FileJavaFieldsConsumer(state, "../java_console/models/src/main/java/com/rusefi/config/generated/" + javaName, baseOffset));
           }

            if (constexpr != null) {
                sdCardFieldsConsumer.home = constexpr;
                sdCardFieldsConsumer.isPtr = isPtr;
                state.addDestination(sdCardFieldsConsumer::handleEndStruct);

                outputValueConsumer.currentSectionPrefix = constexpr;
                outputValueConsumer.conditional = conditional;
                outputValueConsumer.isPtr = isPtr;
                state.addDestination(outputValueConsumer::handleEndStruct);

            }

            {
                ParseState parseState = new ParseState(state.getEnumsReader());

                parseState.setDefinitionPolicy(Definition.OverwritePolicy.NotAllowed);

                if (prepend != null && !prepend.isEmpty()) {
                    RusefiParseErrorStrategy.parseDefinitionFile(parseState.getListener(), prepend);
                }

                RusefiParseErrorStrategy.parseDefinitionFile(parseState.getListener(), state.getDefinitionInputFile());

                CStructWriter cStructs = new CStructWriter();
                cStructs.writeCStructs(parseState, cHeaderDestination);

                // if (outputNames.length == 0) {
                //     outputChannelWriter.writeOutputChannels(parseState, null);
                // } else {
                //     for (int i = 0; i < outputNames.length; i++) {
                //         outputChannelWriter.writeOutputChannels(parseState, outputNames[i]);
                //     }
                // }

                // if (constexpr != null) {
                //     sdLogWriter.writeSdLogs(parseState, constexpr + (isPtr ? "->" : "."));
                // }
            }

            state.doJob();

            fancyNewStuff.append(fragmentDialogConsumer.getContent());

            fancyNewMenu.append(fragmentDialogConsumer.menuLine());

            log.info("Done with " + name + " at " + outputsSections.sensorTsPosition);
        };


        ArrayList<LinkedHashMap<String, Object>> liveDocs = (ArrayList<LinkedHashMap<String, Object>>) data.get("Usages");

        for (LinkedHashMap<String, Object> entry : liveDocs) {
            String name = (String) entry.get("name");
            String java = (String) entry.get("java");
            String folder = (String) entry.get("folder");
            String prepend = (String) entry.get("prepend");
            String constexpr = (String) entry.get("constexpr");
            String conditional = (String) entry.get("conditional_compilation");
            Boolean withCDefines = (Boolean) entry.get("withCDefines");
            Boolean isPtr = (Boolean) entry.get("isPtr");
            // Defaults to false if not specified
            withCDefines = withCDefines != null && withCDefines;
            isPtr = isPtr != null && isPtr;

            Object outputNames = entry.get("output_name");

            String[] outputNamesArr;
            if (outputNames == null) {
                outputNamesArr = new String[0];
            } else if (outputNames instanceof String) {
                outputNamesArr = new String[1];
                outputNamesArr[0] = (String) outputNames;
            } else {
                ArrayList<String> nameList = (ArrayList<String>) outputNames;
                outputNamesArr = new String[nameList.size()];
                nameList.toArray(outputNamesArr);
            }

            handler.onEntry(name, java, folder, prepend, withCDefines, outputNamesArr, constexpr, conditional, isPtr);

            String enumName = "LDS_" + name;
            String type = name + "_s"; // convention
            enumContent.append(enumName + ",\n");

            if (outputNamesArr.length < 2) {
                fragmentsContent
                        .append("decl_frag<")
                        .append(type)
                        .append(">{},\n");
            } else {
                for (int i = 0; i < outputNamesArr.length; i++) {
                    if (i != 0) {
                        // TODO: remove once the rest of the handling for multiple copies of one struct is in place.
                        fragmentsContent.append("// ");
                    }

                    fragmentsContent
                            .append("decl_frag<")
                            .append(type)
                            .append(", ")
                            .append(i)
                            .append(">{},\t// ")
                            .append(outputNamesArr[i])
                            .append("\n");
                }
            }
        }
        enumContent.append("} live_data_e;\n");

        LazyFile lazyFile = new LazyFile("generated/log_fields_generated.h");
        SdCardFieldsConsumer.wrapContent(lazyFile, sdCardFieldsConsumer.getBody());
        lazyFile.close();

        outputValueConsumer.endFile();
        // sdLogWriter.endFile();

        // return outputChannelWriter.getSize();
        return outputsSections.sensorTsPosition;
    }

    private void writeFiles() throws IOException {
        try (FileWriter fw = new FileWriter(enumContentFileName)) {
            fw.write(enumContent.toString());
            fw.write(baseAddressCHeader.toString());
        }

        try (FileWriter fw = new FileWriter("generated/live_data_fragments.h")) {
            fw.write(fragmentsContent.toString());
        }

        String outputPath = "../java_console/io/src/main/java/com/rusefi/enums";
        InvokeReader request = new InvokeReader(outputPath, Collections.singletonList(enumContentFileName));
        EnumToString.handleRequest(request);
    }
}
