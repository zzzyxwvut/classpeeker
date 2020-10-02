package org.zzzyxwvut.classpeeker.internal;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import static org.zzzyxwvut.classpeeker.internal.OptionParser.LauncherOption.ABORT_ON_ERROR;
import static org.zzzyxwvut.classpeeker.internal.OptionParser.LauncherOption.CONCURRENT;
import static org.zzzyxwvut.classpeeker.internal.OptionParser.LauncherOption.READ_FROM;
import static org.zzzyxwvut.classpeeker.internal.OptionParser.LauncherOption.SINGLE;
import static org.zzzyxwvut.classpeeker.internal.OptionParser.LauncherOption.WRITE_TO;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import org.apache.commons.cli.AlreadySelectedException;
import org.apache.commons.cli.MissingArgumentException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import org.zzzyxwvut.classpeeker.internal.OptionParser.LauncherOption;

public class OptionParserTests
{
	@Test
	public void testAbsenceOfUnrecognisableOptions()
	{
		final OptionParser optionParser = assertDoesNotThrow(() ->
			new OptionParser(new String[] { "--unknown", "-u" }));
		assertTrue(optionParser.options().isEmpty());
		assertFalse(optionParser.classNames().isEmpty());
	}

	@Test
	public void testRepeatedOption() /* See OptionParser#parse(Iterator) */
	{
		final OptionParser optionParser = new OptionParser(new String[] {
			"-".concat(READ_FROM.shortName()),
			"fooFile",
			"-".concat(READ_FROM.shortName()),
			"barFile"
		});
		final Set<LauncherOption> expected = Set.of(READ_FROM);
		final Map<LauncherOption, Optional<String>> options =
							optionParser.options();
		final String readFromFile = options.get(READ_FROM)
			.orElse("");
		assertTrue(options.keySet().containsAll(expected));
		assertTrue(optionParser.classNames().isEmpty());
		assertTrue("barFile".equals(readFromFile));
	}

	@Test
	public void testMutuallyExclusiveOptions()
	{
		final Executable optionParser = () ->
					new OptionParser(new String[] {
			"-".concat(SINGLE.shortName()),
			"fooFile",
			"-".concat(WRITE_TO.shortName()),
			"barDir"
		});
		final Optional<Throwable> alreadyError = Optional.ofNullable(
				assertThrows(IllegalStateException.class,
								optionParser)
					.getCause())
			.filter(AlreadySelectedException.class::isInstance);
		assertTrue(alreadyError.isPresent());
	}

	@Test
	public void testSeriesOfOptions()
	{
		final OptionParser optionParser = new OptionParser(new String[] {
			new StringBuilder(4)
				.append("-")
				.append(ABORT_ON_ERROR.shortName())
				.append(CONCURRENT.shortName())
				.append(WRITE_TO.shortName())
				.toString(),
			"fooDir",
			"-".concat(READ_FROM.shortName()),
			"barFile"
		});
		final Set<LauncherOption> expected = Set.of(
			ABORT_ON_ERROR, CONCURRENT, READ_FROM, WRITE_TO);
		final Map<LauncherOption, Optional<String>> options =
						optionParser.options();
		assertTrue(options.keySet().containsAll(expected));
		assertTrue(optionParser.classNames().isEmpty());
		assertTrue("fooDir".equals(options.get(WRITE_TO)
			.orElse("")));
		assertTrue("barFile".equals(options.get(READ_FROM)
			.orElse("")));
	}

	private static Function<LauncherOption, Executable> optionParser()
	{
		return option -> () -> new OptionParser(new String[] {
			"-".concat(option.shortName())
		});
	}

	@SuppressWarnings("NonPublicExported")
	@ParameterizedTest
	@EnumSource(names = { "READ_FROM", "SINGLE", "WRITE_TO" })
	public void testMissingArgument(LauncherOption option)
	{
		final Optional<Throwable> missingError = Optional.ofNullable(
				assertThrows(IllegalStateException.class,
							optionParser()
								.apply(option))
					.getCause())
			.filter(MissingArgumentException.class::isInstance);
		assertTrue(missingError.isPresent());
	}
}
