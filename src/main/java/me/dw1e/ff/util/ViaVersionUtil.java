package me.dw1e.ff.util;

import com.viaversion.viaversion.api.Via;
import me.dw1e.ff.FairFight;
import org.bukkit.entity.Player;

public final class ViaVersionUtil {

    private ViaVersionUtil() {}

    /**
     * 查看玩家游戏版本是否大于1.8.9
     * 虽然做了校验，但使用此方法前最好确保Via存在以避免某些JVM中引起不必要的报错
     * @param p 目标玩家
     * @return 查看结果
     */
    @SuppressWarnings("unchecked")
    public static boolean isPlayerHighVersion(Player p) {
        if (p == null || !p.isOnline()) return false;
        return FairFight.isViaVersionEnabled && Via.getAPI().getPlayerVersion(p) > 47;
    }

}
