package org.zzzyxwvut.classpeeker.internal;

import java.io.Console;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NotDirectoryException;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

import org.zzzyxwvut.classpeeker.ClassData;
import org.zzzyxwvut.classpeeker.internal.ClassPeeker.Failure;
import org.zzzyxwvut.classpeeker.internal.ClassPeeker.None;
import org.zzzyxwvut.classpeeker.internal.ClassPeeker.Result;
import org.zzzyxwvut.classpeeker.internal.OptionParser.LauncherOption;

/** This class serves for running an inspection of classes. */
public class Runner
{
	private Runner() { /* No instantiation. */ }

	private static Function<Function<String, PrintStream>,
				Function<Boolean,
				Predicate<Result>>> resulter()
	{
		return printer -> abortOnError -> result -> {
			printer.apply(result.className())
				.println(result.toString());
			return !(result instanceof None || (abortOnError
					&& result instanceof Failure));
		};
	}

	private static Function<ClassPeeker,
				Function<String, Stream<Result>>> peeker()
	{
		return classPeeker -> classPeeker::peek;
	}

	private static Function<Console, Supplier<String>> nullableReader()
	{
		return console -> () -> console.readLine();
	}

	private static Function<Console, Stream<String>> nullableStreamer()
	{
		return console -> {
			console.format("Type in the fully-qualified"
					+ " name of a class."
					+ "%nPress Ctrl-d (or Ctrl-z)"
					+ " to exit the prompt.%n%n")
				.flush();
			return Stream.generate(nullableReader()
						.apply(console));
		};
	}

	private static Function<Stream<String>, UnaryOperator<Stream<String>>>
								concatenator()
	{
		return leftStream -> rightStream -> Stream.of(leftStream,
								rightStream)
			.flatMap(Function.identity());
	}

	private static Function<Boolean, UnaryOperator<Stream<String>>>
								streamer()
	{
		return inConcurrence -> stream -> (inConcurrence)
			? stream.parallel()
			: stream.sequential();
	}

	private static Function<String, Stream<String>> reader()
	{
		return fileName -> {
			try {
				return Files.lines(Path.of(fileName),
						StandardCharsets.UTF_8);
			} catch (final IOException e) {
				throw new UncheckedIOException(e);
			}
		};
	}

	private static Function<Path, PrintStream> writer()
	{
		return filePath -> {
			try {
				/*
				 * All obtained streams have to be cached so
				 * that the CREATE_NEW invariant is preserved.
				 */
				return new PrintStream(
					Files.newOutputStream(filePath,
						StandardOpenOption.CREATE_NEW,
						StandardOpenOption.WRITE),
					false, /* Flush not every line. */
					StandardCharsets.UTF_8);
			} catch (final IOException e) {
				throw new UncheckedIOException(e);
			}
		};
	}

	private static Function<Path, Function<String, PrintStream>> sinker()
	{
		return writerDirPath -> className -> writer()
			.apply(writerDirPath
				.resolve(Path.of(className)));
	}

	private static Function<Function<String, PrintStream>,
				Function<Map<String, PrintStream>,
				Function<String, PrintStream>>>
							distinctPrinter()
	{
		return sinker -> sinks -> className -> sinks
			.computeIfAbsent(className, sinker);
	}

	private static Function<PrintStream, Function<String, PrintStream>>
							constantPrinter()
	{
		return printStream -> className -> printStream;
	}

	private static Function<String,
				Function<Map<String, PrintStream>,
				Function<Path, PrintStream>>> singleton()
	{
		return className -> sinks -> filePath -> sinks
			.computeIfAbsent(className, constantPrinter()
				.apply(writer()
					.apply(filePath)));
	}

	private static Function<Optional<String>,
				Supplier<UncheckedIOException>> thrower()
	{
		return dirName -> () -> new UncheckedIOException(
					new NotDirectoryException(dirName
			.orElse("Missing directory name")));
	}

	/**
	 * Inspects classes.
	 *
	 * @param classData a non-empty list of class data to collect
	 * @param args an array of command line arguments, if any
	 * @return {@code false} whenever either errors are caught and
	 *	execution is aborted or console is used for input and its
	 *	session is quitted or the help message is requested, else
	 *	{@code true}
	 * @throws IllegalArgumentException if {@code classData} is empty
	 * @throws UncheckedIOException if an I/O error occurs
	 */
	public static boolean inspect(List<? extends ClassData> classData,
								String[] args)
	{
		Objects.requireNonNull(classData, "classData");
		Objects.requireNonNull(args, "args");

		if (classData.isEmpty())
			throw new IllegalArgumentException(
					"No class data elements found");

		final OptionParser optionParser = new OptionParser(args);
		final Map<LauncherOption, Optional<String>> options =
			optionParser.options();

		if (options.containsKey(LauncherOption.HELP)) {
			OptionParser.printUsage(new PrintWriter(System.err),
						classData.get(0).getClass());
			return false;
		}

		final Optional<String> writeDirName = options.getOrDefault(
				LauncherOption.WRITE_TO, Optional.empty());
		final Path writerDirPath = (writeDirName.isPresent())
			? writeDirName
				.map(Path::of)
				.filter(Files::isDirectory)
				.orElseThrow(thrower()
					.apply(writeDirName))
			: null;
		final boolean inConcurrence = options.containsKey(
						LauncherOption.CONCURRENT);
		final Map<String, PrintStream> sinks = (inConcurrence)
			? new ConcurrentHashMap<>()
			: new HashMap<>();
		final Optional<String> singleFileName = options.getOrDefault(
				LauncherOption.SINGLE, Optional.empty());
		final PrintStream printStream = (singleFileName.isPresent())
			? singleFileName
				.map(singleton()
					.apply(Runner.class.getName())
					.apply(sinks)
					.compose(Path::of))
				.orElse(System.out)
			: System.out;
		final List<String> classNames = optionParser.classNames();
		final UnaryOperator<Stream<String>> streamer = streamer()
			.apply(inConcurrence);
		final Optional<String> readFileName = options.getOrDefault(
				LauncherOption.READ_FROM, Optional.empty());

		try {
			return ((readFileName.isPresent())
				? readFileName
					.map(streamer
						.compose(concatenator()
							.apply(classNames
								.stream()))
						.compose(reader()))
					.orElse(Stream.empty())
				: (classNames.isEmpty())
					? Optional.ofNullable(System.console())
						.map(nullableStreamer())
						.orElseThrow(() ->
							new UncheckedIOException(
								new IOException(
									"Not a tty")))
					: streamer.apply(classNames.stream()))
				.flatMap(peeker()
					.apply(new ClassPeeker(classData)))
				.allMatch(resulter()
					.apply((writerDirPath != null)
						? distinctPrinter()
							.apply(sinker()
								.apply(writerDirPath))
							.apply(sinks)
						: constantPrinter()
							.apply(printStream))
					.apply(options.containsKey(
						LauncherOption.ABORT_ON_ERROR)));
		} finally {
			sinks.forEach((k, v) -> v.close());
		}
	}
}
