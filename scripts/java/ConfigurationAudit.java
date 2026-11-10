import com.sun.source.tree.*;
import com.sun.source.util.*;

import javax.tools.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Parses Java with the JDK compiler; no project classes or third-party parser are required.
 */
public class ConfigurationAudit {
    static String quote(String value) {
        if (value == null) return "null";
        StringBuilder out = new StringBuilder("\"");
        for (char c : value.toCharArray())
            switch (c) {
                case '\\' -> out.append("\\\\");
                case '"' -> out.append("\\\"");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> {
                    if (c < 32) out.append(String.format("\\u%04x", (int) c));
                    else out.append(c);
                }
            }
        return out.append('"').toString();
    }

    public static void main(String[] args) throws Exception {
        Path root = Path.of(args.length == 0 ? "." : args[0]).toAbsolutePath().normalize();
        Path output = root.resolve("build/reports/configuration/java-expressions.jsonl");
        Files.createDirectories(output.getParent());
        List<Path> files;
        try (var paths = Files.walk(root.resolve("src/main/java"))) {
            files = paths.filter(p -> p.toString().endsWith(".java")).sorted().toList();
        }
        var compiler = ToolProvider.getSystemJavaCompiler();
        try (var manager = compiler.getStandardFileManager(null, null, StandardCharsets.UTF_8);
             var writer = Files.newBufferedWriter(output, StandardCharsets.UTF_8)) {
            var task = (JavacTask) compiler.getTask(null, manager, diagnostic -> {
                    }, List.of("-proc:none"), null,
                    manager.getJavaFileObjectsFromPaths(files));
            var positions = Trees.instance(task).getSourcePositions();
            for (var unit : task.parse()) {
                String source = unit.getSourceFile().getCharContent(true).toString();
                String file = root.relativize(Path.of(unit.getSourceFile().toUri())).toString().replace('\\', '/');
                new TreePathScanner<Void, Void>() {
                    String method = "";

                    String slice(Tree tree) {
                        long begin = positions.getStartPosition(unit, tree), end = positions.getEndPosition(unit, tree);
                        return begin >= 0 && end >= begin ? source.substring((int) begin, (int) end) : tree.toString();
                    }

                    boolean containsString(Tree tree) {
                        if (tree instanceof LiteralTree literal) return literal.getValue() instanceof String;
                        return tree instanceof BinaryTree binary && tree.getKind() == Tree.Kind.PLUS
                                && (containsString(binary.getLeftOperand()) || containsString(binary.getRightOperand()));
                    }

                    void parts(Tree tree, List<String> values) {
                        if (tree instanceof BinaryTree binary && tree.getKind() == Tree.Kind.PLUS) {
                            parts(binary.getLeftOperand(), values);
                            parts(binary.getRightOperand(), values);
                        } else if (tree instanceof LiteralTree literal && literal.getValue() instanceof String value) {
                            values.add("{\"literal\":" + quote(value) + "}");
                        } else values.add("{\"expr\":" + quote(slice(tree)) + "}");
                    }

                    void emit(Tree tree, String kind) {
                        long start = positions.getStartPosition(unit, tree), finish = positions.getEndPosition(unit, tree);
                        if (start < 0 || finish < start) return;
                        Tree parent = getCurrentPath().getParentPath() == null ? null : getCurrentPath().getParentPath().getLeaf();
                        String parentSource = parent == null ? "" : slice(parent);
                        List<String> values = new ArrayList<>();
                        parts(tree, values);
                        List<String> arguments = new ArrayList<>();
                        String select = "";
                        if (tree instanceof MethodInvocationTree call) {
                            select = call.getMethodSelect().toString();
                            for (Tree argument : call.getArguments()) {
                                var argumentParts = new ArrayList<String>();
                                parts(argument, argumentParts);
                                arguments.add("{\"start\":" + source.codePointCount(0, (int) positions.getStartPosition(unit, argument))
                                        + ",\"end\":" + source.codePointCount(0, (int) positions.getEndPosition(unit, argument))
                                        + ",\"kind\":" + quote(argument.getKind().name()) + ",\"source\":" + quote(slice(argument))
                                        + ",\"parts\":[" + String.join(",", argumentParts) + "]}");
                            }
                        }
                        try {
                            writer.write("{\"file\":" + quote(file) + ",\"start\":" + source.codePointCount(0, (int) start)
                                    + ",\"end\":" + source.codePointCount(0, (int) finish) + ",\"line\":" + unit.getLineMap().getLineNumber(start)
                                    + ",\"kind\":" + quote(kind) + ",\"method\":" + quote(method)
                                    + ",\"select\":" + quote(select) + ",\"arguments\":[" + String.join(",", arguments) + "]"
                                    + ",\"parentKind\":" + quote(parent == null ? "" : parent.getKind().name())
                                    + ",\"parent\":" + quote(parentSource) + ",\"source\":" + quote(slice(tree))
                                    + ",\"parts\":[" + String.join(",", values) + "]}\n");
                        } catch (java.io.IOException e) {
                            throw new java.io.UncheckedIOException(e);
                        }
                    }

                    @Override
                    public Void visitMethod(MethodTree tree, Void unused) {
                        String previous = method;
                        method = tree.getName().toString();
                        super.visitMethod(tree, unused);
                        method = previous;
                        return null;
                    }

                    @Override
                    public Void visitBinary(BinaryTree tree, Void unused) {
                        if (tree.getKind() == Tree.Kind.PLUS && containsString(tree)) emit(tree, "concat");
                        return super.visitBinary(tree, unused);
                    }

                    @Override
                    public Void visitLiteral(LiteralTree tree, Void unused) {
                        if (tree.getValue() instanceof String) emit(tree, "string");
                        else if (tree.getValue() instanceof Number) emit(tree, "number");
                        return super.visitLiteral(tree, unused);
                    }

                    @Override
                    public Void visitMethodInvocation(MethodInvocationTree tree, Void unused) {
                        emit(tree, "call");
                        return super.visitMethodInvocation(tree, unused);
                    }

                    @Override
                    public Void visitReturn(ReturnTree tree, Void unused) {
                        if (tree.getExpression() != null) emit(tree, "return");
                        return super.visitReturn(tree, unused);
                    }
                }.scan(unit, null);
            }
        }
        System.out.println("Parsed " + files.size() + " Java files: " + output);
    }
}
