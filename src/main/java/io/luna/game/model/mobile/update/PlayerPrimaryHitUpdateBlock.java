package io.luna.game.model.mobile.update;

import io.luna.game.model.mobile.Hit;
import io.luna.game.model.mobile.Player;
import io.luna.game.model.mobile.Skill;
import io.luna.game.model.mobile.update.UpdateFlagHolder.UpdateFlag;
import io.luna.net.codec.ByteMessage;
import io.luna.net.codec.ByteTransform;

/**
 * An {@link PlayerUpdateBlock} implementation that handles the primary {@link Hit} update block.
 *
 * @author lare96 <http://github.org/lare96>
 */
public final class PlayerPrimaryHitUpdateBlock extends PlayerUpdateBlock {

    /**
     * Creates a new {@link PlayerPrimaryHitUpdateBlock}.
     */
    public PlayerPrimaryHitUpdateBlock() {
        super(0x20, UpdateFlag.PRIMARY_HIT);
    }

    @Override
    public void write(Player mob, ByteMessage msg) {
        Hit hit = mob.getPrimaryHit();
        Skill hitpoints = mob.skill(Skill.HITPOINTS);

        msg.put(hit.getDamage());
        msg.put(hit.getType().getOpcode(), ByteTransform.A);
        msg.put(hitpoints.getLevel(), ByteTransform.C);
        msg.put(hitpoints.getStaticLevel());
    }
}
