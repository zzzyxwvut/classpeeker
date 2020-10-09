package org.zzzyxwvut.classpeeker;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/** An entry point class. */
class BasicLauncher extends ClassData
{
	private static final List<BasicLauncher> CLASS_DATA = List.of(
		new BasicLauncher(String.format(
				"################################"
				+ "%n%n\tClass/Interface Modifiers:"),
			klass -> new Object[] {
				ClassSupport.classModifiers(klass)
			}),
		new BasicLauncher("Simple Name:",
			klass -> new Object[] {
				klass.getSimpleName()
			}),	/* j.l.String */
		new BasicLauncher("Canonical Name:",
			klass -> new Object[] {
				klass.getCanonicalName()
			}),	/* j.l.String or null */
		new BasicLauncher("Class Loader:",
			klass -> new Object[] {
				klass.getClassLoader()
			}),	/* j.l.ClassLoader or null!*/
		new BasicLauncher("Package:",
			klass -> new Object[] {
				Optional.ofNullable(klass.getPackage())
					.map(Package::getName)
					.orElse(null)
			}),	/* j.l.Package or null */
		new BasicLauncher("Superclass:",
			klass -> new Object[] {
				klass.getSuperclass()
			}),	/* j.l.Class<? super T> or null */
		new BasicLauncher("Generic Superclass:",
			klass -> new Object[] {
				klass.getGenericSuperclass()
			}),	/* j.l.reflect.Type or null!*/
		new BasicLauncher("Declaring Class:",
			klass -> new Object[] {
				klass.getDeclaringClass()
			}),	/* j.l.Class<?> or null!*/
		new BasicLauncher("Generic Interfaces:",
			klass -> klass.getGenericInterfaces()),
				/* j.l.reflect.Type[]!*/
		new BasicLauncher("Interfaces:",
			klass -> klass.getInterfaces()),
				/* j.l.Class[]<?> */
		new BasicLauncher("Annotations:",
			klass -> klass.getAnnotations()),
				/* j.l.annotation.Annotation[] */
		new BasicLauncher("Declared Annotations:",
			klass -> klass.getDeclaredAnnotations()),
				/* j.l.annotation.Annotation[] */
		new BasicLauncher("Classes:",
			klass -> klass.getClasses()),
				/* j.l.Class<?>[]!*/
		new BasicLauncher("Declared Classes:",
			klass -> klass.getDeclaredClasses()),
				/* j.l.Class<?>[]!*/
		new BasicLauncher("Constructors:",
			klass -> klass.getConstructors()),
				/* j.l.reflect.Constructor<?>[]!*/
		new BasicLauncher("Declared Constructors:",
			klass -> klass.getDeclaredConstructors()),
				/* j.l.reflect.Constructor<?>[]!*/
		new BasicLauncher("Methods:",
			klass -> klass.getMethods()),
				/* j.l.reflect.Method[]!*/
		new BasicLauncher("Declared Methods:",
			klass -> klass.getDeclaredMethods()),
				/* j.l.reflect.Method[]!*/
		new BasicLauncher("Fields:",
			klass -> klass.getFields()),
				/* j.l.reflect.Field[]!*/
		new BasicLauncher("Declared Fields:",
			klass -> klass.getDeclaredFields()),
				/* j.l.reflect.Field[]!*/
		new BasicLauncher("Enum Constants:",
			klass -> klass.getEnumConstants()));
				/* T[] or null */

	private BasicLauncher(String description,
				Function<Class<?>, Object[]> methodist)
	{
		super(description, methodist);
	}

	/**
	 * Inspects classes.
	 *
	 * @param args an array of command line arguments, if any
	 */
	public static void main(String[] args)
	{
		ClassSupport.inspect(CLASS_DATA, args);
	}
}
