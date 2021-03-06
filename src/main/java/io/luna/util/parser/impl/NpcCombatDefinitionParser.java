package io.luna.util.parser.impl;

import com.google.gson.JsonObject;
import io.luna.game.model.def.NpcCombatDefinition;
import io.luna.util.GsonUtils;
import io.luna.util.parser.GsonParser;

import java.util.List;
import java.util.Map;

/**
 * A {@link GsonParser} implementation that reads {@link NpcCombatDefinition}s.
 *
 * @author lare96 <http://github.org/lare96>
 */
public class NpcCombatDefinitionParser extends GsonParser<NpcCombatDefinition> {

    /**
     * The {@link Map} that will contain parsed {@link NpcCombatDefinition}s.
     */
    private final Map<Integer, NpcCombatDefinition> definitions;

    /**
     * Creates a new {@link NpcCombatDefinitionParser}.
     *
     * @param definitions The {@link Map} that will contain parsed {@link NpcCombatDefinition}s.
     */
    public NpcCombatDefinitionParser(Map<Integer, NpcCombatDefinition> definitions) {
        super("./data/npcs/npc_combat_defs.json");
        this.definitions = definitions;
    }

    @Override
    public NpcCombatDefinition readObject(JsonObject reader) throws Exception {
        int id = reader.get("id").getAsInt();
        int respawnTicks = reader.get("respawn_ticks").getAsInt();
        boolean aggressive = reader.get("aggressive").getAsBoolean();
        boolean poisonous = reader.get("poisonous").getAsBoolean();
        int combatLevel = reader.get("level").getAsInt();
        int hitpoints = reader.get("hitpoints").getAsInt();
        int maximumHit = reader.get("maximum_hit").getAsInt();
        int attackSpeed = reader.get("attack_speed").getAsInt();
        int attackAnimation = reader.get("attack_animation").getAsInt();
        int defenceAnimation = reader.get("defence_animation").getAsInt();
        int deathAnimation = reader.get("death_animation").getAsInt();
        int[] skills = GsonUtils.getAsType(reader.get("skills"), int[].class);
        int[] bonuses = GsonUtils.getAsType(reader.get("bonuses"), int[].class);

        return new NpcCombatDefinition(id, respawnTicks, aggressive, poisonous, combatLevel, hitpoints, maximumHit,
            attackSpeed, attackAnimation, defenceAnimation, deathAnimation, skills, bonuses);
    }

    @Override
    public void onReadComplete(List<NpcCombatDefinition> readObjects) throws Exception {
        readObjects.forEach(it -> definitions.put(it.getId(), it));
    }
}
