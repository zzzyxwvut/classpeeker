package org.zzzyxwvut.classpeeker.internal;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.zzzyxwvut.classpeeker.ClassData;
import org.zzzyxwvut.classpeeker.internal.ClassPeeker.Failure;
import org.zzzyxwvut.classpeeker.internal.ClassPeeker.None;
import org.zzzyxwvut.classpeeker.internal.ClassPeeker.Result;
import org.zzzyxwvut.classpeeker.internal.ClassPeeker.Success;

public class ClassPeekerTests
{
	private ClassPeeker classPeeker;

	@BeforeEach
	public void setUp()
	{
		classPeeker = new ClassPeeker(DummyClassData.CLASS_DATA);
	}

	@Test
	public void testPeekWithNull()
	{
		final Result result = classPeeker.peek(null)
			.findAny()
			.orElseThrow();
		assertTrue(result instanceof None);
	}

	@Test
	public void testPeekWithEmptyClassName()
	{
		final Result result = classPeeker.peek("")
			.findAny()
			.orElseThrow();
		assertTrue(result instanceof Failure);
		final Exception e = ((Failure) result).exception();
		assertTrue(e instanceof IllegalArgumentException);
		assertTrue(e.getMessage().startsWith("Empty class name:"));
	}

	@Test
	public void testPeekWithFullyQualifiedFormClassName()
	{
		final List<Result> result = classPeeker.peek(
						getClass().getName())
			.collect(Collectors.toUnmodifiableList());
		assertTrue(result.size() == DummyClassData.CLASS_DATA.size());

		assertTrue(result.get(0) instanceof Success);
		final Success success0 = (Success) result.get(0);
		assertTrue("Simple Name:".equals(success0.description()));
		assertTrue(getClass().getSimpleName()
			.equals(success0.classData()[0]));

		assertTrue(result.get(1) instanceof Success);
		final Success success1 = (Success) result.get(1);
		assertTrue("TODO #1:".equals(success1.description()));
		assertNull(success1.classData()[0]);

		assertTrue(result.get(2) instanceof Success);
		final Success success2 = (Success) result.get(2);
		assertTrue("TODO #2:".equals(success2.description()));
		assertNull(success2.classData());

		assertTrue(result.get(3) instanceof Failure);
		final Exception e = ((Failure) result.get(3)).exception();
		assertTrue(e instanceof UnsupportedOperationException);
		assertTrue(e.getMessage().equals("Oops!"));
	}

	@Test
	public void testPeekWithClassPathFormClassName()
	{
		final List<Result> result = classPeeker.peek(
						getClass().getName()
							.replace('.', '/')
							.concat(".class"))
			.collect(Collectors.toUnmodifiableList());
		assertTrue(result.size() == DummyClassData.CLASS_DATA.size());
		assertTrue(result.get(0) instanceof Success);
		assertTrue(result.get(1) instanceof Success);
		assertTrue(result.get(2) instanceof Success);
		assertTrue(result.get(3) instanceof Failure);
	}

	@Test
	public void testPeekWithMixedFormClassName()
	{
		final List<Result> result = classPeeker.peek(
						getClass().getName()
							.replace('.', '/'))
			.collect(Collectors.toUnmodifiableList());
		assertTrue(result.size() == DummyClassData.CLASS_DATA.size());
		assertTrue(result.get(0) instanceof Success);
		assertTrue(result.get(1) instanceof Success);
		assertTrue(result.get(2) instanceof Success);
		assertTrue(result.get(3) instanceof Failure);
	}

	static class DummyClassData extends ClassData
	{
		static final List<DummyClassData> CLASS_DATA = List.of(
			new DummyClassData("Simple Name:",
				klass -> new Object[] { klass.getSimpleName() }),
			new DummyClassData("TODO #1:",
				klass -> new Object[] { null }),
			new DummyClassData("TODO #2:", klass -> null),
			new DummyClassData("TODO #3:", klass -> {
				throw new UnsupportedOperationException("Oops!");
			}));

		private DummyClassData(String description,
					Function<Class<?>, Object[]> methodist)
		{
			super(description, methodist);
		}
	}
}
