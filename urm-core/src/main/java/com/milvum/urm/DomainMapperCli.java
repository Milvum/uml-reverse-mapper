package com.milvum.urm;

import com.milvum.urm.domain.*;
import com.milvum.urm.presenters.*;
import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;
import org.apache.commons.cli.*;
import org.slf4j.*;

public class DomainMapperCli {

    private static final Logger log = LoggerFactory.getLogger(DomainMapperCli.class);
    DomainMapper domainMapper;

    public static void main(final String[] args) throws ClassNotFoundException, IOException {
        new DomainMapperCli().run(args);
    }

    public void run(final String[] args) throws ClassNotFoundException, IOException {
        CommandLineParser parser = new DefaultParser();
        Options options = new Options();
        options.addOption(Option.builder("h").longOpt("help").desc("displays this help text").build());
        options.addOption(Option.builder("f").argName("file").hasArg()
            .desc("write result to file instead of console").build());
        options.addOption(Option.builder("s").longOpt("presenter").argName("[plant|graph]").hasArg()
            .desc("presenter to be used, defaults to 'plant'").build());

        options.addOption(Option.builder("u").argName("fileList").hasArgs()
            .valueSeparator(',')
            .desc("comma separated list of jars or other java files to load").build());
        options.addOption(Option.builder("p").argName("packageList").hasArgs()
            .required()
            .valueSeparator(',')
            .desc("comma separated list of domain packages").build());
        options.addOption(Option.builder("i").longOpt("classIgnores").argName("ignoreList").hasArgs()
            .valueSeparator(',')
            .desc("comma separated list of ignored types").build());
        options.addOption(Option.builder("fi").longOpt("fieldIgnores").argName("ignoreList").hasArgs()
            .valueSeparator(',')
            .desc("comma separated list of ignored fields, defaults to '$jacocoData'").build());
        options.addOption(Option.builder("mi").longOpt("methodIgnores").argName("ignoreList").hasArgs()
            .valueSeparator(',')
            .desc("comma separated list of ignored methods, defaults to '$jacocoInit'").build());

        try {
            CommandLine cl = parser.parse(options, args);

            if (cl.hasOption("-h")) {
                throw new ParseException("help invoked");
            }

            if (cl.hasOption("fi")) {
                DomainClass.IGNORED_FIELDS = Arrays.asList(cl.getOptionValues("fi"));
            }
            if (cl.hasOption("mi")) {
                DomainClass.IGNORED_METHODS = Arrays.asList(cl.getOptionValues("mi"));
            }
            ClassLoader additionalClassLoader = null;
            if (cl.hasOption("u")) {
                String[] uStrings = cl.getOptionValues("u");
                URL[] urlArray = Arrays.stream(uStrings)
                    .map(s -> {
                        try {
                            return new File(s).toURI().toURL();
                        } catch (MalformedURLException e) {
                            e.printStackTrace();
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .toArray(URL[]::new);
                additionalClassLoader = new URLClassLoader(urlArray, getClass().getClassLoader());
            }

            String[] packages = cl.getOptionValues("p");
            log.debug("Scanning domain for packages: " + Arrays.toString(packages));
            String[] ignores = null;
            if (cl.hasOption("i")) {
                ignores = cl.getOptionValues("i");
                if (ignores != null) {
                    log.debug("Ignored types:");
                    for (String ignore : ignores) {
                        log.debug(ignore);
                    }
                }
            }
            Presenter presenter = Presenter.parse(cl.getOptionValue("i"));
            domainMapper = DomainMapper.create(presenter, Arrays.asList(packages),
                ignores == null ? new ArrayList<>() : Arrays.asList(ignores),
                additionalClassLoader);
            Representation representation = domainMapper.describeDomain();
            if (cl.hasOption('f')) {
                String filename = cl.getOptionValue('f');
                Files.write(Paths.get(filename), representation.getContent().getBytes());
                log.info("Wrote to file " + filename);
            } else {
                log.info(representation.getContent());
            }
        } catch (ParseException exp) {
            log.info(exp.getMessage());
            // automatically generate the help statement
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("java -jar urm-core.jar", "", options,
                "EXAMPLES:\n"
                + "1. java -jar urm-core.jar -p com.iluwatar.abstractfactory "
                + "-i com.iluwatar.abstractfactory.Castle -u abstract-factory.jar\n"
                + "2. java -cp abstract-factory.jar:urm-core.jar DomainMapperCli "
                + "-p com.iluwatar.abstractfactory -i com.iluwatar.abstractfactory.Castle\n");
        }
    }
}
