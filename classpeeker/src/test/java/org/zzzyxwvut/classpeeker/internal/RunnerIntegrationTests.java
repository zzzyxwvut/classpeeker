package org.zzzyxwvut.classpeeker.internal;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import static org.zzzyxwvut.classpeeker.internal.OptionParser.LauncherOption.ABORT_ON_ERROR;
import static org.zzzyxwvut.classpeeker.internal.OptionParser.LauncherOption.CONCURRENT;
import static org.zzzyxwvut.classpeeker.internal.OptionParser.LauncherOption.READ_FROM;
import static org.zzzyxwvut.classpeeker.internal.OptionParser.LauncherOption.SINGLE;
import static org.zzzyxwvut.classpeeker.internal.OptionParser.LauncherOption.WRITE_TO;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.cli.MissingArgumentException;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import org.zzzyxwvut.impedimenta.FileReader;

import org.zzzyxwvut.classpeeker.ClassData;
import org.zzzyxwvut.classpeeker.internal.OptionParser.LauncherOption;

public class RunnerIntegrationTests
{
	private static final List<String> CLASS_NAMES = List.of("byte",
						"java.io.Serializable",
						"java/lang/Object",
						"java/util/Map.class");
	private static final Map<String, String> DATA = new LinkedHashMap<>();

	/* ClassPeeker.Success#toString() */
	private static final byte[] EXPECTED;
	private static final byte[] EXPECTED_WITH_FAILURE;

	static {
		DATA.put("byte",
			String.format("%n\tSimple Name:%nbyte%n"));
		DATA.put("java.io.Serializable",
			String.format("%n\tSimple Name:%nSerializable%n"));
		DATA.put("java.lang.Object",
			String.format("%n\tSimple Name:%nObject%n"));
		DATA.put("java.util.Map",
			String.format("%n\tSimple Name:%nMap%n"));
		EXPECTED = DATA.values()
			.stream()
			.sequential()
			.collect(Collectors.joining())
			.getBytes(StandardCharsets.UTF_8);
		EXPECTED_WITH_FAILURE = DATA.values()
			.stream()
			.sequential()
			.collect(Collectors.joining(
					FailureClassData.FAILURE_ENTRY,
					FailureClassData.FAILURE_ENTRY, ""))
			.getBytes(StandardCharsets.UTF_8);
	}

	@TempDir
	public static Path tmpDirPath;

	private static Path readFromPath;

	@BeforeAll
	public static void setUpClass() throws IOException
	{
		readFromPath = tmpDirPath.resolve(Path.of("readFromPath"));
		Files.write(readFromPath, CLASS_NAMES, StandardCharsets.UTF_8,
						StandardOpenOption.CREATE_NEW,
						StandardOpenOption.WRITE);
	}

	private static Function<List<? extends ClassData>,
				Function<String[], Executable>> runner()
	{
		return classData -> args -> () -> Runner.inspect(classData,
								args);
	}

	private static Function<Class<? extends Exception>,
				Predicate<Throwable>> instancer()
	{
		return cause -> cause::isInstance;
	}

	private static Optional<Throwable> failingRunner(
					List<? extends ClassData> classData,
					String[] args,
					Class<? extends Exception> exception,
					Class<? extends Exception> cause)
	{
		return Optional.ofNullable(assertThrows(exception,
						runner().apply(classData)
							.apply(args))
				.getCause())
			.filter(instancer()
				.apply(cause));
	}

	@Test
	public void testNoOptionsReadFromSystemIn() throws IOException
	{
		final InputStream newStream = Files.newInputStream(
				readFromPath, StandardOpenOption.READ);
		final InputStream oldStream = System.in;

		try {
			System.setIn(newStream);
			final Optional<Throwable> notATtyError = failingRunner(
						DummyClassData.CLASS_DATA,
						new String[0],
						UncheckedIOException.class,
						IOException.class);
			assertTrue(notATtyError.isPresent());
		} finally {
			System.setIn(oldStream);
			newStream.close();
		}
	}

	@Test
	public void testOptionAbortingReadFromFileWriteToFile() throws
								IOException
	{
		final Path singlePath = tmpDirPath.resolve(
			Path.of("testOptionAbortingReadFromFileWriteToFile"));
		final boolean success = Runner.inspect(
						FailureClassData.CLASS_DATA,
							new String[] {
			"-".concat(ABORT_ON_ERROR.shortName()),
			"-".concat(READ_FROM.shortName()),
			readFromPath.toString(),
			"-".concat(SINGLE.shortName()),
			singlePath.toString()
		});
		assertFalse(success);
		assertArrayEquals(FailureClassData.FAILURE_ENTRY
			.getBytes(StandardCharsets.UTF_8),
						new FileReader()
			.watchAndReadBytes(singlePath, 64));
	}

	@Test
	public void testOptionNotAbortingReadFromFileWriteToFile() throws
								IOException
	{
		final Path singlePath = tmpDirPath.resolve(
			Path.of("testOptionNotAbortingReadFromFileWriteToFile"));
		final boolean success = Runner.inspect(
						FailureClassData.CLASS_DATA,
							new String[] {
			"-".concat(READ_FROM.shortName()),
			readFromPath.toString(),
			"-".concat(SINGLE.shortName()),
			singlePath.toString()
		});
		assertTrue(success);
		assertArrayEquals(EXPECTED_WITH_FAILURE, new FileReader()
			.watchAndReadBytes(singlePath, 512));
	}

	@Test
	public void testOptionReadFromFileWriteToFile() throws IOException
	{
		final Path singlePath = tmpDirPath.resolve(
			Path.of("testOptionReadFromFileWriteToFile"));
		final boolean success = Runner.inspect(
						DummyClassData.CLASS_DATA,
							new String[] {
			"-".concat(READ_FROM.shortName()),
			readFromPath.toString(),
			"-".concat(SINGLE.shortName()),
			singlePath.toString()
		});
		assertTrue(success);
		assertArrayEquals(EXPECTED, new FileReader()
			.watchAndReadBytes(singlePath, 128));
	}

	@Test
	public void testOptionReadFromFileConcurrentWriteToDirectory() throws
					IOException, InterruptedException
	{
		final Path tmpTmpDirPath = Files.createTempDirectory(
			tmpDirPath,
			"testOptionReadFromFileConcurrentWriteToDirectory");
		final boolean success = Runner.inspect(
						DummyClassData.CLASS_DATA,
							new String[] {
			"-".concat(READ_FROM.shortName()),
			readFromPath.toString(),
			"-".concat(CONCURRENT.shortName()),
			"-".concat(WRITE_TO.shortName()),
			tmpTmpDirPath.toString()
		});
		assertTrue(success);
		assertTrue(FileReader.watchUntilCreated(tmpTmpDirPath));
		assertTrue(DATA.entrySet()
			.stream()
			.allMatch(collator()
				.apply(tmpTmpDirPath)));
	}

	private static Function<Path, Predicate<Entry<String, String>>>
								collator()
	{
		return path -> entry -> Arrays.equals(entry.getValue()
			.getBytes(StandardCharsets.UTF_8),
							new FileReader()
			.watchAndReadBytes(path.resolve(entry.getKey()), 64));
	}

	@Test
	public void testReadingCommandLineOptionWriteToFile() throws IOException
	{
		final Path singlePath = tmpDirPath.resolve(
			Path.of("testReadingCommandLineOptionWriteToFile"));
		final boolean success = Runner.inspect(
						DummyClassData.CLASS_DATA,
							new String[] {
			"-".concat(SINGLE.shortName()),
			singlePath.toString(),
			RunnerIntegrationTests.class.getName()
		});
		assertTrue(success);
		final byte[] expected = String.format(
				"%n\tSimple Name:%n%s%n",
				RunnerIntegrationTests.class.getSimpleName())
			.getBytes(StandardCharsets.UTF_8);
		assertArrayEquals(expected, new FileReader()
			.watchAndReadBytes(singlePath, 64));
	}

	@Test
	public void testReadingCommandLineOptionReadFromFileWriteToFile() throws
								IOException
	{
		final Path singlePath = tmpDirPath.resolve(
			Path.of("testReadingCommandLineOptionReadFromFileWriteToFile"));
		final boolean success = Runner.inspect(
						DummyClassData.CLASS_DATA,
							new String[] {
			"-".concat(READ_FROM.shortName()),
			readFromPath.toString(),
			"-".concat(SINGLE.shortName()),
			singlePath.toString(),
			RunnerIntegrationTests.class.getName()
		});
		assertTrue(success);
		final byte[] expected = String.format(
				"%n\tSimple Name:%n%s%n%s",
				RunnerIntegrationTests.class.getSimpleName(),
				new String(EXPECTED, StandardCharsets.UTF_8))
			.getBytes(StandardCharsets.UTF_8);
		assertArrayEquals(expected, new FileReader()
			.watchAndReadBytes(singlePath, 256));
	}

	@SuppressWarnings("NonPublicExported")
	@ParameterizedTest
	@EnumSource(names = { "READ_FROM", "SINGLE", "WRITE_TO" })
	public void testOptionWithMissingRequiredArgument(LauncherOption option)
	{
		final Optional<Throwable> missingError = failingRunner(
						DummyClassData.CLASS_DATA,
			new String[] {
				"-".concat(option.shortName())
			},
			IllegalStateException.class,
			MissingArgumentException.class);
		assertTrue(missingError.isPresent());
	}

	@AfterAll
	public static void tearDownClass() throws IOException
	{
		Files.list(tmpDirPath)
			.flatMap(path -> {
				try {
					return (Files.isDirectory(path))
						? Files.list(path)
						: Stream.of(path);
				} catch (final IOException e) {
					throw new UncheckedIOException(e);
				}
			})
			.sorted()
			.forEach(path -> System.err.println(path));
		final List<String> classNames = Files.readAllLines(
				readFromPath, StandardCharsets.UTF_8);
		assertIterableEquals(CLASS_NAMES, classNames);
	}

	static class DummyClassData extends ClassData
	{
		static final List<DummyClassData> CLASS_DATA = List.of(
			new DummyClassData("Simple Name:",
				klass -> new Object[] {
					klass.getSimpleName()
			}));

		private DummyClassData(String description,
					Function<Class<?>, Object[]> methodist)
		{
			super(description, methodist);
		}
	}

	static class FailureClassData extends ClassData
	{
		static final String FAILURE_ENTRY = String.format("%s%n",
				new UnsupportedOperationException("Oops!")
			.toString());
		static final List<FailureClassData> CLASS_DATA = List.of(
			new FailureClassData("TODO:",
				klass -> {
					throw new UnsupportedOperationException(
								"Oops!");
				}),
			new FailureClassData("Simple Name:",
				klass -> new Object[] {
					klass.getSimpleName()
				}));

		private FailureClassData(String description,
					Function<Class<?>, Object[]> methodist)
		{
			super(description, methodist);
		}
	}
}
