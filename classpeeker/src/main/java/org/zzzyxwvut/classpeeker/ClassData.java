package org.zzzyxwvut.classpeeker;

import java.util.Objects;
import java.util.function.Function;

/** Instances of this class may produce some class data. */
public abstract class ClassData
{
	private final String description;
	private final Function<Class<?>, Object[]> methodist;

	/**
	 * Constructs a new {@code ClassData} object.
	 *
	 * @param description the description of the passed {@code methodist}
	 *	functional interface
	 * @param methodist a functional interface that takes a class and
	 *	returns either an array of objects or {@code null}
	 */
	protected ClassData(String description,
				Function<Class<?>, Object[]> methodist)
	{
		this.description = Objects.requireNonNull(description,
							"description");
		this.methodist = Objects.requireNonNull(methodist,
							"methodist");
	}

	/**
	 * Returns the description of the methodist functional interface.
	 *
	 * @return the description of the methodist functional interface
	 */
	public String description()			{ return description; }

	/**
	 * Returns the functional interface that takes a class and returns
	 * either an array of objects or {@code null}.
	 *
	 * @return the functional interface that takes a class and returns
	 *	either an array of objects or {@code null}
	 */
	public Function<Class<?>, Object[]> methodist()	{ return methodist; }
}
