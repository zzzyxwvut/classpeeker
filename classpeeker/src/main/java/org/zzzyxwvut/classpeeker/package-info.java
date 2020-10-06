/**
 * Provides class inspection API.
 * <p>
 * (The "class" term denotes instances of classes proper (with enums),
 * interfaces (with annotations), arrays, {@code void}, and primitive
 * types.)
 * <p>
 * This project can be extended as follows:
 * <ul>
 * <li>Derive an entry point class from {@link org.zzzyxwvut.classpeeker.ClassData}
 * and in its {@code main} method invoke
 * {@link org.zzzyxwvut.classpeeker.ClassSupport#inspect(java.util.List, String[])}.
 * </li>
 * <li>Create a {@code src/main/resources/application.properties} file and
 * define {@code classpeeker.example-1.find}, {@code classpeeker.bundle.jar},
 * and {@code classpeeker.group} properties that shall be used with the help
 * message (that observes the 80-characters-a-line width), e.g.
 * <pre><code> classpeeker.group=${project.groupId}
 * classpeeker.bundle.jar=${project.build.finalName}-jar-with-dependencies-no-module-info.jar
 *
 * ## The value of the find property is treated as a format string,
 * ## see java.util.Formatter#format(String, Object...)
 * classpeeker.example-1.find=find ~/.m2/repository/commons-cli \
 * -name commons-cli-${commons-cli.version}.jar</code></pre>
 * </li>
 * </ul>
 */
package org.zzzyxwvut.classpeeker;
