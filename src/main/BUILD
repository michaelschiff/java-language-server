load("@rules_java//java:defs.bzl", "java_binary")
load("@rules_jvm_external//:defs.bzl", "artifact")
java_binary(
	name = "java-language-server", 
	main_class = "org.javacs.Main",
	srcs = glob(["**/*.java"]),
	deps = [
		artifact("com.google.code.gson:gson"),
		artifact("com.google.protobuf:protobuf-java"),
	],
	javacopts = [
		"--add-exports jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED",
		"--add-exports jdk.compiler/com.sun.tools.javac.code=ALL-UNNAMED",
		"--add-exports jdk.compiler/com.sun.tools.javac.comp=ALL-UNNAMED",
		"--add-exports jdk.compiler/com.sun.tools.javac.main=ALL-UNNAMED",
		"--add-exports jdk.compiler/com.sun.tools.javac.tree=ALL-UNNAMED",
		"--add-exports jdk.compiler/com.sun.tools.javac.model=ALL-UNNAMED",
		"--add-exports jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED",
	],
	add_opens = [
		"jdk.compiler/com.sun.tools.javac.api",
	],
	add_exports = [
		"jdk.compiler/com.sun.tools.javac.api",
		"jdk.compiler/com.sun.tools.javac.code",
		"jdk.compiler/com.sun.tools.javac.comp",
		"jdk.compiler/com.sun.tools.javac.main",
		"jdk.compiler/com.sun.tools.javac.tree",
		"jdk.compiler/com.sun.tools.javac.model",
		"jdk.compiler/com.sun.tools.javac.util",
	]
)

