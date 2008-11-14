(jde-project-file-version "1.0")
(jde-set-variables
 '(jde-project-name "JMeter RMI protocol plugin")
 '(jde-gen-buffer-boilerplate (quote ("/*" " *" " *" " * See README in the source tree for more info" " *" " */" " ")))
 '(jde-ant-enable-find t)
 '(jde-build-function '(jde-ant-build))
 '(jde-lib-directory-names '("^lib" "^jar" "^java" "^plugins" "^ext"))
 '(jde-global-classpath
   (list "/usr/share/java"
         "./lib"
         "~/apps/jmeter/lib/ext"
         "./build/main"
         "./build/test"))
 '(jde-gen-k&r nil)
 '(tab-width 4)
 '(jde-sourcepath '("./src" "./test")))
