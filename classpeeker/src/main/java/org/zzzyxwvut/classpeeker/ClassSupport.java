package org.zzzyxwvut.classpeeker;

import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Objects;

import org.zzzyxwvut.classpeeker.internal.Runner;

/** This class lends class-related support. */
public class ClassSupport
{
	private ClassSupport() { /* No instantiation. */ }

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
	 * @throws java.io.UncheckedIOException if an I/O error occurs
	 */
	public static boolean inspect(List<? extends ClassData> classData,
								String[] args)
	{
		return Runner.inspect(classData, args);
	}

	/**
	 * Returns a string representation of all class modifiers found.
	 *
	 * @param klass an instance of a class
	 * @return a string representation of all class modifiers found
	 */
	public static String classModifiers(Class<?> klass)
	{
		Objects.requireNonNull(klass, "klass");
		final int mm = klass.getModifiers();
		return new StringBuilder(64)
			.append(((mm & Modifier.PUBLIC) != 0)
				? "public, "
				: ((mm & Modifier.PROTECTED) != 0)
					? "protected, "
					: ((mm & Modifier.PRIVATE) != 0)
						? "private, "
						: "")
			.append(((mm & Modifier.ABSTRACT) != 0)
				? ((mm & Modifier.STATIC) != 0)
					? "abstract, static, "
					: "abstract, "
				: ((mm & Modifier.FINAL) != 0)
					? ((mm & Modifier.STATIC) != 0)
						? "static, final, "
						: "final, "
					: ((mm & Modifier.STATIC) != 0)
						? "static, "
						: "")
			.append(((mm & Modifier.STRICT) != 0)
				? "strictfp, "
				: "")
			.append((klass.isSynthetic())
				? "synthetic, "
				: "")
			.append((klass.isAnnotation())
				? "@interface"
				: ((mm & Modifier.INTERFACE) != 0)
					? "interface"
					: (klass.isEnum())
						? "enum"
////						: (klass.isRecord())
////							? "record"
							: "class")
			.toString();
	}	/* See JLS-11, $[89].1.1 */
}
