RMI Sampler and Proxy for Apache JMeter
=======================================

[![Build Status](https://travis-ci.org/jmibanez/jmeter-rmi-plugin.svg?branch=develop)](https://travis-ci.org/jmibanez/jmeter-rmi-plugin)

An Apache JMeter plugin for recording and performing RMI calls.


## Requirements ##

This plugin needs Java 8 and Apache Ant >1.8 to build; it runs on
Apache JMeter 3.2 onwards.


## Building ##

To download project dependencies:
```
    $ ant bootstrap
```

To build and run tests:
```
    $ ant build-rmi
    $ ant test
```

To package JAR for installation:
```
    $ ant jar
```


## Installation ##

1. Copy ApacheJMeter_rmi.jar to `lib/ext` in your JMeter installation
1. Copy your application's dependencies and any JAR files containing
   relevant classes to `lib/` in your JMeter installation
1. Run JMeter.


## Classpath and Dependencies ##

Currently, the plugin depends on your application's classes being in
JAR files in the `lib` folder of your JMeter installation; it does not
rely on the RMI class loader. This may be changed in the future.

## RMI Socket Factories ##

This plugin is compatible with any RMI SocketFactory that your
application provides, and will use those transparently as per the RMI
specification.

## Recording RMI Calls ##

To record your application's RMI calls, you must point your
application to the registry exposed by the RMI Proxy provided by this
plugin.

1. Add the `RMI Proxy` to your workbench.
1. Change `Target RMI Name` to point to your application's RMI
   server. The proxy can currently only bind to a single named remote,
   but will also monitor and track remotes returned by methods of that
   named remote.
1. Configure the proxy's registry port by changing `Proxy Naming
   Port`. The default should be fine.
1. Configure the port where the proxy's equivalent remote instance
   will listen in. This may be important if you need to punch holes on
   your firewall for RMI.
1. Optionally, if your application code needs some configuration for
   serialization (e.g. setting certain static variables as
   configuration, etc.), you can provide a BeanShell script that will
   be evaluated once the proxy has started and looked up the remote
   object at `Target RMI Name`. See Binding Script below for more
   info.
1. Click `Start` to start the proxy.
1. Run your application, pointing to the RMI name configured for the
   proxy.

For example, if your application's RMI server exposes a remote object
at `//192.168.56.1:1099/myService`, you can use the following settings:

  * Target RMI Name: `//192.168.56.1:1099/myService`
  * Proxy Naming Port: `1100`
  * Proxy Port: `1101`
  * RMI Name for your application `//<your local IP address>:1100/myService`


### Binding Script ###

To configure any necessary static variables your application may need
to be set beforehand, for instance due to logic in your Serializable
classes, you can provide a *binding script*. The binding script on the
RMI Proxy is a BeanShell scriptlet that is evaluated at binding time
(i.e. when the proxy has started, and looked up your remote instance,
and replaced it with a recording proxy). The following variables are
available within the context of the binding script:

  * `proxy`: The proxy of the remote instance, provided by the
    plugin. Any method call made by your scriptlet on this object will
    be recorded by the plugin, as though your application itself made
    a similar call;
  * `serverStub`: The remote instance from your application's RMI
    server. Calls made by your scriptlet on this object will bypass
    the plugin and will not be recorded;


## Making JMeter Perform RMI Calls ##

Every RMI method call on a remote object is represented by an instance
of an RMI Sampler. The provided RMI Proxy will record all calls made
by your application to the remote object, and create the corresponding
RMI Sampler instances.

Each RMI Sampler has the following properties:

  * `Target name`: The target remote instance; an empty value
    represents the root instance (See below for info about non-root
    remotes);
  * `Method name`: The mangled name of the method to invoke;
  * `Ignore Exceptions`: Whether to treat exceptions as sample
    failures or not -- if marked unchecked, any `RemoteException`
    thrown by the server will be treated as a sample failure;
  * `Arguments script`: A BeanShell script to construct the arguments
    needed to invoke the method; this script **must** contain a
    definition for a method named `methodArgs` that returns an
    `Object[]` array containing the arguments of the method.

The value for the `Method name` parameter is created by mangling the
name of the method and its arguments, as follows:

  * The method name, followed by a colon (`:`);
  * The `Class.getName()` of each argument's class, separated by commas (`,`)

For instance, a method named `foo`, having the following signature:

```
    String doFoo(String a, char[] b, int[] c, boolean d);
```

will be mangled as `doFoo:java.lang.String,[C,[I,boolean`.

Similarly, a method named having no arguments named `quux`, will be
mangled as `quux:` (note the trailing colon).

### The RMI Remote Object Config ###

You should have noticed that the RMI Sampler does not have a direct
reference to an RMI remote object or its URL
(e.g. `//192.168.56.1:1099/fooService`). This is by design. You must
add a *RMI Remote Object Config* element to your test plan before any
RMI Sampler can be used. The RMI Remote Object Config has one
property, the `Target RMI name`, which is the URL to the root remote
object that the methods specified by the RMI Samplers will be invoked
against.

Note that for non-root remote objects (i.e. those returned by methods
on remote objects), the RMI Remote Object Config is also used to
maintain references to them. See below for more info.


### The Arguments Script ###

To construct the arguments for a the method invoked by the RMI
Sampler, a BeanShell script must be provided that defines a single
method named `methodArgs`, which returns an `Object[]` array
containing the arguments to the method. This script is initially
evaluated at test start. Whenever the sampler is actually run to take
the RMI sample, the defined `methodArgs()` method is invoked and its
return value is passed to the configured method as its arguments.

The following variables are available within the context of the
argument script:

  * `ctx`: The `JMeterContext` of the currently executing Thread;
  * `vars`: JMeter user-defined variables;
  * `sampler`: The RMI sampler itself.


### Non-Root Remotes ###

It is possible for a method on a remote object to itself return
another remote object, either directly as the return value itself, or
indirectly as part of the object graph of the return value.

The RMI Sampler does not automatically persist these remote
objects. It is your responsibility to save the remote object, and in
turn register them with the RMI Remote Object Config. You can do this
through a BeanShell Post Processor element attached to the relevant
RMI Sampler. Do note that if you are recording RMI method calls
through the RMI Proxy, this will be done for you automatically.

To register the remote object, add a BeanShell Post Processor with a
script like so:

```
    Object ret = prev.getReturnValue();
    com.jmibanez.tools.jmeter.InstanceRegistry reg = vars.getObject("RMIRemoteObject.instances");
    // REPLACE_ME: name of remote in registry 
    // ret: actual object path to Remote
    if (reg.getTarget("myRemote") == null) {
    	reg.registerRmiInstance("myRemote", ret);
    }
```

The `String` first argument to `registerRmiInstance` is the value that
the RMI Sampler references in the `Target name` property.

As mentioned earlier, if you leave the `Target name` property empty
for an RMI Sampler, it will use the root remote object, specified by
the relevant RMI Remote Object config.


## Known Issues ##

### Recording/RMI Proxy ###

#### When generating argument scriptlets, the RMI Proxy does not track instances ####

When generating argument scriptlets, the RMI proxy currently does not
track object instances across method invocations. For instance, if
method `foo` is invoked by the instrumented application with an object
of class `Bar`, and the same `Bar` instance is then passed to method
`fooTwo`, the argument scripts generated by the RMI proxy for the two
RMI samplers will each create a different `Bar` instance, instead of
saving the `Bar` instance in the first call (e.g. in the JMeter
variables) and referencing it in the second call.

This should not be a major issue, as arguments are passed by value
under RMI, unless the object is a `Remote`, in which case it is passed
by reference. For the general case (non-`Remote` objects), there is no
semantic difference when used as arguments to methods between creating
two separate instances who have the same content (and are equal)
versus using the same instance.

#### The RMI proxy can't properly handle remotes passed by client ####

Currently, the RMI proxy does not support the application passing a
remote object to the server for the purposes of the server calling
into the client. The proxy does forward the remote properly to the
server; however, the generated argument script is broken, and
references into the dynamic proxy that the JVM generates as the stub
in lieu of the remote.



## Copyright and License ##

Copyright © 2009 Orange and Bronze Software Labs; Copyright ©
2009-2017 JM Ibanez. Licensed under the GNU GPL version 2. See the
file [LICENSE.txt](LICENSE.txt) for more info.
