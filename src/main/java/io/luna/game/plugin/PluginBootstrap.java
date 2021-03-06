package io.luna.game.plugin;

import com.google.common.collect.FluentIterable;
import com.google.common.io.Files;
import com.google.gson.JsonObject;
import com.moandjiezana.toml.Toml;
import io.luna.LunaContext;
import io.luna.game.event.EventListenerPipelineSet;
import io.luna.util.GsonUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import scala.Console;
import scala.tools.nsc.Settings;
import scala.tools.nsc.interpreter.IMain;
import scala.tools.nsc.settings.MutableSettings.BooleanSetting;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.apache.logging.log4j.util.Unbox.box;

/**
 * A bootstrapper that initializes and evaluates all {@code Scala} dependencies and plugins.
 *
 * @author lare96 <http://github.org/lare96>
 */
public final class PluginBootstrap implements Callable<EventListenerPipelineSet> {

    /**
     * A {@link ByteArrayOutputStream} implementation that intercepts output from the {@code Scala} interpreter and forwards
     * the output to {@code System.err} when there is an evaluation error.
     */
    private final class ScalaConsole extends ByteArrayOutputStream {

        @Override
        public synchronized void flush() {
            Pattern pattern = Pattern.compile("<console>:([0-9]+): error:");

            String output = toString();
            Matcher matcher = pattern.matcher(output);

            reset();

            if (matcher.find()) {
                String fileName = currentFile.get();
                String message = output.substring(output.indexOf(':', 10) + 8);
                LOGGER.fatal("Error while interpreting plugin file \"{}\" {}{}", fileName, System.lineSeparator(), message);
            }
        }
    }

    /**
     * The asynchronous logger.
     */
    private static final Logger LOGGER = LogManager.getLogger();

    /**
     * The directory that contains all files related to plugins.
     */
    private static final String DIR = "./plugins/";

    /**
     * The {@link EventListenerPipelineSet} that listeners from plugins will be added to.
     */
    private final EventListenerPipelineSet pipelines = new EventListenerPipelineSet();

    /**
     * A {@link Map} of the file names in {@code DIR} to their contents.
     */
    private final Map<String, String> files = new HashMap<>();

    /**
     * The current file that is being evaluated.
     */
    private final AtomicReference<String> currentFile = new AtomicReference<>();

    /**
     * The {@link LunaContext} that will be used to inject state into plugins.
     */
    private final LunaContext context;

    /**
     * The {@link ScriptEngine} that will evaluate the {@code Scala} scripts.
     */
    private final ScriptEngine engine;

    /**
     * Creates a new {@link PluginBootstrap}.
     *
     * @param context The {@link LunaContext} that will be used to inject state into plugins.
     */
    public PluginBootstrap(LunaContext context) {
        this.context = context;
        engine = new ScriptEngineManager().getEngineByName("scala");
    }

    @Override
    public EventListenerPipelineSet call() throws Exception {
        init();
        LOGGER.info("A total of {} Scala plugin files were successfully interpreted.", box(files.size()));
        return pipelines;
    }

    /**
     * Initializes this bootstrapper, loading all of the plugins.
     */
    public void init() throws Exception {
        PrintStream oldConsole = Console.out();
        ScalaConsole newConsole = new ScalaConsole();

        Console.setOut(new PrintStream(newConsole));
        try {
            initClasspath();
            initFiles();
            initDependencies();
            initPlugins();
        } finally {
            Console.setOut(oldConsole);
        }
    }

    /**
     * Configures the {@code Scala} interpreter to use the {@code Java} classpath.
     */
    private void initClasspath() throws Exception {
        IMain interpreter = (IMain) engine;
        Settings settings = interpreter.settings();
        BooleanSetting booleanSetting = (BooleanSetting) settings.usejavacp();

        booleanSetting.value_$eq(true);
    }

    /**
     * Parses all of the files in {@code DIR} and caches their contents into {@code files}.
     */
    private void initFiles() throws Exception {
        FluentIterable<File> dirFiles = Files.fileTreeTraverser().preOrderTraversal(new File(DIR)).filter(File::isFile);

        for (File file : dirFiles) {
            files.put(file.getName(), Files.toString(file, StandardCharsets.UTF_8));
        }
    }

    /**
     * Injects state into the {@code engine} and evaluates dependencies from {@code DIR}.
     */
    private void initDependencies() throws Exception {
        engine.put("ctx: io.luna.LunaContext", context);
        engine.put("logger: org.apache.logging.log4j.Logger", LOGGER);
        engine.put("pipelines: io.luna.game.event.EventListenerPipelineSet", pipelines);

        Toml toml = new Toml().read(files.remove("dependencies.toml"));
        JsonObject reader = toml.getTable("dependencies").to(JsonObject.class);
        String parentDependency = reader.get("parent_dependency").getAsString();
        String[] childDependencies = GsonUtils.getAsType(reader.get("child_dependencies"), String[].class);

        currentFile.set(parentDependency);
        engine.eval(files.remove(parentDependency));
        for (String dependency : childDependencies) {
            currentFile.set(dependency);
            engine.eval(files.remove(dependency));
        }
    }

    /**
     * Evaluates all of the dependant plugins from within {@code DIR}.
     */
    private void initPlugins() throws Exception {
        for (Entry<String, String> fileEntry : files.entrySet()) {
            currentFile.set(fileEntry.getKey());
            engine.eval(fileEntry.getValue());
        }
    }
}
