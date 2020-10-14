package org.zzzyxwvut.classpeeker.internal;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionGroup;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

/** This class serves for parsing command line arguments. */
class OptionParser
{
	private static final Options OPTIONS = new Options()
		.addOption(LauncherOption.HELP.shortName(),
			LauncherOption.HELP.longName(),
			LauncherOption.HELP.requiresArgument(),
			LauncherOption.HELP.description())
		.addOption(LauncherOption.ABORT_ON_ERROR.shortName(),
			LauncherOption.ABORT_ON_ERROR.longName(),
			LauncherOption.ABORT_ON_ERROR.requiresArgument(),
			LauncherOption.ABORT_ON_ERROR.description())
		.addOption(LauncherOption.CONCURRENT.shortName(),
			LauncherOption.CONCURRENT.longName(),
			LauncherOption.CONCURRENT.requiresArgument(),
			LauncherOption.CONCURRENT.description())
		.addOption(LauncherOption.READ_FROM.shortName(),
			LauncherOption.READ_FROM.longName(),
			LauncherOption.READ_FROM.requiresArgument(),
			LauncherOption.READ_FROM.description())
		.addOptionGroup(new OptionGroup()
			.addOption(Option
				.builder(LauncherOption.SINGLE.shortName())
				.longOpt(LauncherOption.SINGLE.longName())
				.hasArg(LauncherOption.SINGLE
							.requiresArgument())
				.desc(LauncherOption.SINGLE.description())
				.build())
			.addOption(Option
				.builder(LauncherOption.WRITE_TO.shortName())
				.longOpt(LauncherOption.WRITE_TO.longName())
				.hasArg(LauncherOption.WRITE_TO
							.requiresArgument())
				.desc(LauncherOption.WRITE_TO.description())
				.argName("DIRNAME")
				.build()));

	private final Map<LauncherOption, Optional<String>> options;
	private final List<String> classNames;

	/**
	 * Constructs a new {@code OptionParser} object.
	 *
	 * @param args an array of command line arguments, if any
	 */
	OptionParser(String[] args)
	{
		Objects.requireNonNull(args, "args");

		try {
			if (args.length > 0) {
				final CommandLine commandLine =
							new DefaultParser()
					.parse(OPTIONS, args, true);
				options = parse(commandLine.iterator());
				classNames = commandLine.getArgList();
			} else {
				options = Map.of();
				classNames = List.of();
			}
		} catch (final ParseException e) {
			throw new IllegalStateException(e);
		}
	}

	private static Map<LauncherOption, Optional<String>> parse(
					Iterator<Option> optionIterator)
	{
		return Map.copyOf(StreamSupport
			.stream(Spliterators.spliterator(optionIterator, 0,
				Spliterator.IMMUTABLE | Spliterator.NONNULL),
				false)
			.collect(Collectors.toMap(
				option -> LauncherOption.fromString(
							option.getOpt())
					.orElseThrow(IllegalStateException::new),
				option -> Optional.ofNullable(
							option.getValue()),
				(oldValue, newValue) -> newValue,
				HashMap::new)));
	}

	/**
	 * Returns a map of parsed options with their values, if any.
	 *
	 * @return a map of parsed options with their values, if any
	 */
	Map<LauncherOption, Optional<String>> options()	{ return options; }

	/**
	 * Returns a list of parsed class names, if any.
	 *
	 * @return a list of parsed class names, if any
	 */
	List<String> classNames()			{ return classNames; }

	/**
	 * Delivers the help message.
	 *
	 * @param writer a writer to deliver the help message to
	 * @param klass an entry point class
	 */
	static void printUsage(PrintWriter writer, Class<?> klass)
	{
		Objects.requireNonNull(writer, "writer");
		Objects.requireNonNull(klass, "klass");
		fetchProperties(klass);
		final String padding = "    ";		/* 4 SPACEs (0x20). */
		final String cmdLineSyntax = String.format(
			"java -jar $(find ~/.m2/repository/%1$s/ -name \\"
			+ "%n%2$s)",
			System.getProperty("classpeeker.group", "")
				.replace('.', '/'),
			System.getProperty("classpeeker.bundle.jar",
						"classpeeker-bundle.jar"));
		final String header = String.format(
				"%1$s%<s[fully.qualified.ClassName "
				+ "fully/qualified/ClassName[.class] ...]"
			+ "%n%nAn inspector of classes."
			+ "%n%nOptions:", padding);
		final String footer = String.format("%nExamples:"
			+ "%n%1$sjava -cp $(%2$s) %3$s \\"
			+ "%norg.apache.commons.cli.HelpFormatter"
			+ "%n%n%1$s%4$s \\"
			+ "%n\\[\\[B void"
			+ "%n%n%1$s%4$s \\"
			+ "%n-%5$s /tmp/URI.txt java.net.URI java.net.URI\\$1 "
				+ "java.net.URI\\$Parser"
			+ "%n%n%1$smkdir /tmp/jdk-11 &&"
			+ "%n%1$stime %4$s \\"
			+ "%n-%6$s%7$s%8$s /tmp/jdk-11 -%9$s "
				+ "\"${OPENJDK:?}\"/lib/classlist",
			padding,
			String.format(System.getProperty(
					"classpeeker.example-1.find", ":")),
			klass.getName(),
			cmdLineSyntax,
			LauncherOption.SINGLE.shortName(),
			LauncherOption.ABORT_ON_ERROR.shortName(),
			LauncherOption.CONCURRENT.shortName(),
			LauncherOption.WRITE_TO.shortName(),
			LauncherOption.READ_FROM.shortName());
		final HelpFormatter formatter = new HelpFormatter();
		formatter.setArgName("FILENAME");
		formatter.setSyntaxPrefix(String.format("Usage:%n%s", padding));
		formatter.printHelp(writer, 80, cmdLineSyntax, header,
						OPTIONS, 4, 4, footer, true);
		writer.flush();
	}

	private static void fetchProperties(Class<?> klass)
	{
		final String resourceName = "/application.properties";
		final Properties props;

		try (InputStream is = klass.getResourceAsStream(
							resourceName)) {
			if (is == null)
				throw new IllegalArgumentException(
					String.format(
						"Unavailable resource: '%s'",
						resourceName));

			try (InputStreamReader isr = new InputStreamReader(is,
						StandardCharsets.UTF_8);
					BufferedReader br =
						new BufferedReader(isr)) {
				props = new Properties();
				props.load(br);
			}
		} catch (final IOException e) {
			throw new UncheckedIOException(e);
		}

		for (String name : props.stringPropertyNames())
			System.setProperty(name, props.getProperty(name));
	}

	/** The options supported by an entry point class. */
	enum LauncherOption
	{
		/** An option of using default settings. */
		DEFAULT("", "", false, "Use default settings"),

		/** An option of printing the help message and exiting. */
		HELP("h", "help", false, "Print the help message and exit"),

		/**
		 * An option of aborting further inspection of a class on
		 * error.
		 * <p>
		 * Note that the default guarantees for aborting are skewed
		 * for a concurrent inspection.
		 */
		ABORT_ON_ERROR("a", "abort-on-error", false,
			"Abort further inspection of a class on error"),

		/** An option of collecting data concurrently. */
		CONCURRENT("c", "concurrent", false,
			"Collect data concurrently"),

		/** An option of reading class names from a file. */
		READ_FROM("r", "read-from-file", true,
			"Read class names from a file"),

		/** An option of writing all collected data to a new file. */
		SINGLE("s", "single-file", true,
			"Write ALL collected data to a NEW file"),

		/** An option of writing each class data to a new file. */
		WRITE_TO("w", "write-to-directory", true,
			"Write EACH class data to a NEW file");

		private static final Map<String, LauncherOption> NAMES =
						Arrays.stream(values())
			.collect(Collectors.toMap(LauncherOption::shortName,
						Function.identity()));

		private final String shortName;
		private final String longName;
		private final boolean requiresArgument;
		private final String description;

		private LauncherOption(String shortName, String longName,
				boolean requiresArgument, String description)
		{
			this.shortName = shortName;
			this.longName = longName;
			this.requiresArgument = requiresArgument;
			this.description = description;
		}

		/**
		 * Returns the short name of an option.
		 *
		 * @return the short name of an option
		 */
		String shortName()		{ return shortName; }

		/**
		 * Returns the long name of an option.
		 *
		 * @return the long name of an option
		 */
		String longName()		{ return longName; }

		/**
		 * Returns whether the option requires an argument.
		 *
		 * @return whether the option requires an argument
		 */
		boolean requiresArgument()	{ return requiresArgument; }

		/**
		 * Returns the description of an option.
		 *
		 * @return the description of an option
		 */
		String description()		{ return description; }

		/**
		 * Returns an optional with the corresponding enumeration
		 * instance.
		 *
		 * @param shortName the short name of an option
		 * @return an optional with the corresponding enumeration
		 *	instance, otherwise an empty optional
		 */
		static Optional<LauncherOption> fromString(String shortName)
		{
			Objects.requireNonNull(shortName, "shortName");
			return Optional.ofNullable(NAMES.get(shortName));
		}
	}
}
