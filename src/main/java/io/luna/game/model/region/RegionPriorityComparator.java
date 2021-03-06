package io.luna.game.model.region;

import com.google.common.collect.ImmutableList;
import fj.P2;
import io.luna.LunaConstants;
import io.luna.game.model.mobile.MobileEntity;
import io.luna.game.model.mobile.Player;

import java.util.Comparator;
import java.util.function.BiFunction;

import static fj.P.p;

/**
 * A {@link Comparator} implementation that ensures that important mobs are added to the local list of a player before other
 * (less important) mobs.
 *
 * @author lare96 <http://github.org/lare96>
 * @see LunaConstants#STAGGERED_UPDATING
 */
public final class RegionPriorityComparator implements Comparator<MobileEntity> {

    /**
     * A makeshift type alias for the verbose and confusing {@link BiFunction} type declaration.
     */
    @FunctionalInterface
    private interface RegionFactor extends BiFunction<MobileEntity, MobileEntity, P2<Integer, Integer>> {
        /* It's times like this I miss Scala, :( could just do:
             type RegionFactor = (MobileEntity, MobileEntity) => (Int, Int) */
    }

    /**
     * An {@link ImmutableList} of the factors that will be used to compare mobs.
     */
    private final ImmutableList<RegionFactor> factors = ImmutableList
        .of(this::comparePosition, this::compareSize, this::compareCombatLevel, this::compareCombat);

    /**
     * The {@link Player} being updated.
     */
    private final Player player;

    /**
     * Creates a new {@link RegionPriorityComparator}.
     *
     * @param player The {@link Player} being updated.
     */
    public RegionPriorityComparator(Player player) {
        this.player = player;
    }

    @Override
    public int compare(MobileEntity left, MobileEntity right) {
        int leftFactor = 0;
        int rightFactor = 0;

        for (RegionFactor factor : factors) {
            P2<Integer, Integer> tuple = factor.apply(left, right);
            leftFactor += tuple._1();
            rightFactor += tuple._2();
        }

        if (leftFactor == rightFactor) {
            leftFactor++;
        }
        return Integer.compare(leftFactor, rightFactor);
    }

    /**
     * Compare mob positions. Awards {@code 1} point factor.
     */
    private P2<Integer, Integer> comparePosition(MobileEntity left, MobileEntity right) {
        int leftDistance = player.distanceFrom(left);
        int rightDistance = player.distanceFrom(right);
        return compareValues(leftDistance, rightDistance, 1);
    }

    /**
     * Compare mob sizes. Awards {@code 1} point factor.
     */
    private P2<Integer, Integer> compareSize(MobileEntity left, MobileEntity right) {
        int leftSize = left.size();
        int rightSize = right.size();
        return compareValues(leftSize, rightSize, 1);
    }

    /**
     * Compare mob combat levels. Awards {@code 2} point factors.
     */
    private P2<Integer, Integer> compareCombatLevel(MobileEntity left, MobileEntity right) {
        int leftCombatLevel = left.getCombatLevel();
        int rightCombatLevel = right.getCombatLevel();
        return compareValues(leftCombatLevel, rightCombatLevel, 2);
    }

    /**
     * Compare mob combat states. Awards {@code 3} point factors.
     */
    private P2<Integer, Integer> compareCombat(MobileEntity left, MobileEntity right) { // TODO finish when combat is done
        int leftCombat = /* left.inCombat() ? 1 :*/ 0;
        int rightCombat = /* right.inCombat() ? 1 :*/ 0;
        return compareValues(leftCombat, rightCombat, 3);
    }

    /**
     * Compares the input values and awards points based on a which one is greater.
     */
    private P2<Integer, Integer> compareValues(int left, int right, int points) {
        if (left > right) {
            return p(points, 0);
        } else if (right > left) {
            return p(0, points);
        } else {
            return p(points, points);
        }
    }
}
