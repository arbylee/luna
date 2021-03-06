package io.luna.net.session;

import io.luna.game.model.mobile.Player;
import io.luna.net.LunaNetworkConstants;
import io.luna.net.codec.IsaacCipher;
import io.luna.net.msg.GameMessage;
import io.luna.net.msg.MessageReader;
import io.luna.net.msg.MessageRepository;
import io.luna.net.msg.MessageWriter;
import io.netty.channel.Channel;

import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;

/**
 * A {@link Session} implementation that handles networking for a {@link Player} during gameplay.
 *
 * @author lare96 <http://github.org/lare96>
 */
public final class GameSession extends Session {

    /**
     * The player assigned to this {@code GameSession}.
     */
    private final Player player;

    /**
     * The message encryptor.
     */
    private final IsaacCipher encryptor;

    /**
     * The message decryptor.
     */
    private final IsaacCipher decryptor;

    /**
     * The repository containing data for incoming messages.
     */
    private final MessageRepository messageRepository;

    /**
     * A bounded queue of inbound {@link GameMessage}s.
     */
    private final Queue<GameMessage> inboundQueue = new ArrayBlockingQueue<>(LunaNetworkConstants.MESSAGE_LIMIT);

    /**
     * Creates a new {@link GameSession}.
     *
     * @param channel The channel for this session.
     * @param encryptor The message encryptor.
     * @param decryptor The message decryptor.
     * @param messageRepository The repository containing data for incoming messages.
     */
    public GameSession(Player player, Channel channel, IsaacCipher encryptor, IsaacCipher decryptor,
        MessageRepository messageRepository) {
        super(channel);
        this.player = player;
        this.encryptor = encryptor;
        this.decryptor = decryptor;
        this.messageRepository = messageRepository;
    }

    @Override
    public void onDispose() {
        player.getWorld().queueLogout(player);
    }

    @Override
    public void handleUpstreamMessage(Object msg) {
        if (msg instanceof GameMessage) {
            inboundQueue.offer((GameMessage) msg);
        }
    }

    /**
     * Writes {@code msg} to the underlying channel; The channel is not flushed.
     *
     * @param msg The message to queue.
     */
    public void queue(MessageWriter msg) {
        Channel channel = getChannel();

        if (channel.isActive()) {
            channel.write(msg.handleOutboundMessage(player), channel.voidPromise());
        }
    }

    /**
     * Flushes all pending {@link GameMessage}s within the channel's queue. Repeated calls to this method are relatively
     * expensive, which is why messages should be queued up with {@code queue(MessageWriter)} and flushed once at the end of
     * the cycle.
     */
    public void flushQueue() {
        Channel channel = getChannel();

        if (channel.isActive()) {
            channel.flush();
        }
    }

    /**
     * Dequeues the inbound queue, handling all logic accordingly.
     */
    public void dequeue() {
        for (; ; ) {
            GameMessage msg = inboundQueue.poll();
            if (msg == null) {
                break;
            }
            MessageReader inbound = messageRepository.getHandler(msg.getOpcode());
            inbound.handleInboundMessage(player, msg);
        }
    }

    /**
     * @return The message encryptor.
     */
    public IsaacCipher getEncryptor() {
        return encryptor;
    }

    /**
     * @return The message decryptor.
     */
    public IsaacCipher getDecryptor() {
        return decryptor;
    }
}
