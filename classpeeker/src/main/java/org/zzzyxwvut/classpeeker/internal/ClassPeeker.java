package org.zzzyxwvut.classpeeker.internal;

import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.zzzyxwvut.classpeeker.ClassData;

/** This class serves for class inspection. */
class ClassPeeker
{
	private static final Pattern DOT_SLASH_CLASS = Pattern.compile(
						"(?:^\\.+|/|\\.(?:class)??$)");
	private static final Map<String, Class<?>> PRIMITIVES_AND_VOID =
			Map.of(Boolean.TYPE.getName(),		boolean.class,
				Byte.TYPE.getName(),		byte.class,
				Character.TYPE.getName(),	char.class,
				Double.TYPE.getName(),		double.class,
				Float.TYPE.getName(),		float.class,
				Integer.TYPE.getName(),		int.class,
				Long.TYPE.getName(),		long.class,
				Short.TYPE.getName(),		short.class,
				Void.TYPE.getName(),		void.class);

	private final Function<String, Stream<Result>> resulter;

	/**
	 * Constructs a new {@code ClassPeeker} object.
	 *
	 * @param classData a list of class data to collect
	 */
	ClassPeeker(List<? extends ClassData> classData)
	{
		Objects.requireNonNull(classData, "classData");
		resulter = resulter()
			.apply(classicist()
				.apply(getClass().getClassLoader()))
			.apply(classData);
	}

	private static Function<Class<?>,
				Function<ClassData, Result>> peeker()
	{
		return klass -> classData -> {
			try {
				return new Success(klass.getName(),
						classData.description(),
						classData.methodist()
							.apply(klass));
			} catch (final Exception e) {
				return new Failure(klass.getName(), e);
			}
		};
	}

	private static Function<ClassLoader,
				Function<String, Supplier<Class<?>>>>
								classicist()
	{
		return classLoader -> className -> () -> {
			try {
				return Class.forName(className, false,
								classLoader);
			} catch (final ClassNotFoundException e) {
				throw new UncheckedROE(e);
			}
		};
	}

	private static Function<Function<String, Supplier<Class<?>>>,
				Function<List<? extends ClassData>,
				Function<String, Stream<Result>>>> resulter()
	{
		return classicist -> classData -> className -> {
			final Class<?> klass;

			try {
				klass = Objects.requireNonNullElseGet(
					PRIMITIVES_AND_VOID.get(className),
					classicist.apply(className));
			} catch (final UncheckedROE e) {
				return Stream.of(new Failure(className,
							e.getCause()));
			}

			return classData
				.stream()
				.map(peeker()
					.apply(klass));
		};
	}

	/**
	 * Collects class data.
	 *
	 * @param className a class name in either its fully-qualified form,
	 *	e.g. {@code org.example.Foo}; or a class-path form, e.g.
	 *	{@code org/example/Foo.class}; or a mixture of both, e.g.
	 *	{@code org/example/Foo}
	 * @return a stream with the none result, if the passed class name
	 *	is {@code null}, else a stream of results that are either
	 *	collected class data or errors
	 */
	Stream<Result> peek(String className)
	{
		return (className == null)
			? Stream.of(None.instance())
			: (className.isBlank())
				? Stream.of(new Failure(className,
					new IllegalArgumentException(
							String.format(
						"Empty class name: '%s'",
						className))))
				: resulter.apply(DOT_SLASH_CLASS
					.splitAsStream(className)
					.filter(Predicate.not(String::isBlank))
					.collect(Collectors.joining(".")));
	}

	/**
	 * This interface exposes some result of class inspection or its
	 * absence.
	 */
	interface Result
	{
		/**
		 * Returns the name of a class under inspection.
		 *
		 * @return the name of a class under inspection
		 */
		String className();
	}

	/** This class exposes a successful result of class inspection. */
	static final class Success implements Result
	{
		private final String className;
		private final String description;
		private final Object[] classData;

		/**
		 * Constructs a new {@code Success} object.
		 *
		 * @param className the name of a class under inspection
		 * @param description the description of collected class data
		 * @param classData an array of collected class data, or
		 *	{@code null}
		 */
		Success(String className, String description,
							Object[] classData)
		{
			this.className = Objects.requireNonNull(className,
								"className");
			this.description = Objects.requireNonNull(description,
								"description");
			this.classData = classData;
		}

		/**
		 * Returns the description of collected class data.
		 *
		 * @return the description of collected class data
		 */
		String description()		{ return description; }

		/**
		 * Returns the array of collected class data, or {@code null}.
		 *
		 * @return the array of collected class data, or {@code null}
		 */
		Object[] classData()		{ return classData; }

		@Override
		public String className()	{ return className; }

		@Override
		public String toString()
		{
			final String newline = System.lineSeparator();
			return Stream.ofNullable(classData)
				.flatMap(Stream::of)
				.filter(Objects::nonNull)
				.map(Object::toString)
				.collect(Collectors.joining(newline,
					new StringBuilder(32)
						.append(newline)
						.append("\t")
						.append(description)
						.append(newline),
					""));
		}
	}

	/** This class exposes a failed result of class inspection. */
	static final class Failure implements Result
	{
		private final String className;
		private final Exception exception;

		/**
		 * Constructs a new {@code Failure} object.
		 *
		 * @param className the name of a class under inspection
		 * @param exception a collected exception
		 */
		Failure(String className, Exception exception)
		{
			this.className = Objects.requireNonNull(className,
								"className");
			this.exception = Objects.requireNonNull(exception,
								"exception");
		}

		/**
		 * Returns the collected exception.
		 *
		 * @return the collected exception
		 */
		Exception exception()		{ return exception; }

		@Override
		public String className()	{ return className; }

		@Override
		public String toString()
		{
			return Objects.requireNonNullElse(exception.getCause(),
					exception)
				.toString();
		}
	}

	/** This class exposes an absence of class inspection. */
	static final class None implements Result
	{
		private static final None NONE = new None();

		private None() { /* No instantiation. */ }

		/**
		 * Obtains the {@code None} object.
		 *
		 * @return the {@code None} object
		 */
		static None instance()		{ return NONE; }

		@Override
		public String className()	{ return ""; }

		@Override
		public String toString()	{ return ""; }
	}

	/**
	 * Instances of this class wrap a {@code ReflectiveOperationException}
	 * with an unchecked exception.
	 */
	static final class UncheckedROE extends RuntimeException
	{
		private static final long serialVersionUID = 1L;

		/**
		 * Constructs a new {@code UncheckedROE} object.
		 *
		 * @param cause a {@code ReflectiveOperationException}
		 */
		UncheckedROE(ReflectiveOperationException cause)
		{
			super(Objects.requireNonNull(cause, "cause"));
		}

		@Override
		public ReflectiveOperationException getCause()
		{
			return (ReflectiveOperationException) super.getCause();
		}

		private void readObject(ObjectInputStream stream) throws
					IOException, ClassNotFoundException
		{
			stream.defaultReadObject();

			if (!(super.getCause() instanceof
						ReflectiveOperationException))
				throw new InvalidObjectException(
					"Not a ReflectiveOperationException");
		}
	}
}
