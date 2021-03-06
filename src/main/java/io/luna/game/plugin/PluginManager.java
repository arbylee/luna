package io.luna.game.plugin;

import io.luna.LunaContext;
import io.luna.game.event.Event;
import io.luna.game.event.EventListener;
import io.luna.game.event.EventListenerPipeline;
import io.luna.game.event.EventListenerPipelineSet;
import io.luna.game.model.mobile.Player;

/**
 * A manager for Scala plugins. It uses {@link EventListener}s and {@link EventListenerPipeline}s to act as a bridge between
 * interpreted Scala code and compiled Java code.
 *
 * @author lare96 <http://github.org/lare96>
 */
public final class PluginManager {

    /**
     * An {@link EventListenerPipelineSet} containing the event types and designated pipelines.
     */
    private final EventListenerPipelineSet pipelines = new EventListenerPipelineSet();

    /**
     * An instance of the {@link LunaContext}.
     */
    private final LunaContext context;

    /**
     * Creates a new {@link PluginManager}.
     *
     * @param context The context for this {@code PluginManager}.
     */
    public PluginManager(LunaContext context) {
        this.context = context;
    }

    /**
     * Attempts to traverse {@code evt} across its designated {@link EventListenerPipeline}.
     *
     * @param evt The event to post.
     * @param player The {@link Player} to post this event for, if intended to be {@code null} use {@code post(Event)}
     * instead.
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void post(Event evt, Player player) {
        EventListenerPipeline pipeline = pipelines.retrievePipeline(evt.getClass());

        if (pipeline == null) {
            return;
        }
        pipeline.traverse(evt, player);
    }

    /**
     * The equivalent to {@code post(Event, Player)}, except uses {@code null} for the {@link Player} argument.
     *
     * @param evt The event to post.
     */
    public void post(Event evt) {
        post(evt, null);
    }

    /**
     * @return An instance of the {@link LunaContext}.
     */
    public LunaContext getContext() {
        return context;
    }

    /**
     * @return An {@link EventListenerPipelineSet} containing the event types and designated pipelines.
     */
    public EventListenerPipelineSet getPipelines() {
        return pipelines;
    }
}
