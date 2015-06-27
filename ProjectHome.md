## Introduction ##

JVM Monitor is a Java profiler integrated with Eclipse to monitor CPU, threads and memory usage of Java applications.

![http://svn.codespot.com/a/eclipselabs.org/jvmmonitor/trunk/org.jvmmonitor.doc/screenshots.png](http://svn.codespot.com/a/eclipselabs.org/jvmmonitor/trunk/org.jvmmonitor.doc/screenshots.png)

JVM Monitor would be useful to quickly inspect Java applications without preparing any launch configuration beforehand. JVM Monitor automatically finds the running JVMs on local host and you can easily start monitoring them. It is also supported to monitor Java applications on remote host by giving hostname and port number.

If further deep analysis is needed, you may use other tools (e.g. TPTP, Memory Analyzer) as a next step.

![http://svn.codespot.com/a/eclipselabs.org/jvmmonitor/trunk/org.jvmmonitor.doc/domain.png](http://svn.codespot.com/a/eclipselabs.org/jvmmonitor/trunk/org.jvmmonitor.doc/domain.png)

## Software Requirements ##

| **Platform** | Windows, Linux, or Mac OS X |
|:-------------|:----------------------------|
| **Eclipse**  | Helios 3.6.x, Indigo 3.7.x or Juno 3.8.x/4.2.x |
| **Java for Eclipse** | Oracle JDK 6 or 7, OpenJDK 6 or 7, or Apple JDK 6 |
| **Java to be profiled** | Oracle JDK/JRE 6 or 7, OpenJDK 6 or 7, or Apple JDK 6 |

## Installation ##

There are several options to install.

Option 1) open Eclipse Marketplace wizard (Help > Eclipse Marketplace...), search with the text 'JVM Monitor', and click Install button.

Option 2) If the Eclipse Marketplace menu is not found, open Install wizard (Help > Install New Software... > Add...), and enter the following update site.

```
http://www.jvmmonitor.org/updates/3.8
```

Option 3) manually install by downloading [zip file](http://code.google.com/a/eclipselabs.org/p/jvmmonitor/downloads/list) and unzipping in Eclipse dropins folder.

**Note**: JVM Monitor is implemented only with pure Java and doesn't provide any native library/executable unlike some other profilers, so the installation is simply to place plugins at Eclipse plugins directory without messing up your system. That's why the option (3) works.

## Getting Started ##

See the [online documentation](http://www.jvmmonitor.org/doc/index.html#Getting_started).

## News ##

  * Feb 2, 2013: 3.8.1 has been released
  * May 12, 2012: Eclipse 3.8.x / 4.2.x (Juno) is supported in 3.8.0
  * Aug 17, 2011: Java SE 7 is supported in 3.7.1
  * Jun 19, 2011: Eclipse 3.7 (Indigo) is supported in 3.7.0
  * Mar 13, 2011: development status is now 'beta' in 3.6.9
  * Dec 26, 2010: Mac OS X is supported in 3.6.8
  * Dec 14, 2010: Windows 64-bit is supported in 3.6.7
  * Nov 13, 2010: added to Eclipse Marketplace in 3.6.5
  * Oct 01, 2010: initial version 3.6.0 has been released