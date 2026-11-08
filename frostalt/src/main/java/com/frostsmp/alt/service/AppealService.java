package com.frostsmp.alt.service;

import com.frostsmp.alt.config.PluginConfig;
import com.frostsmp.alt.model.AltCase;
import com.frostsmp.alt.model.PlayerStatus;
import com.frostsmp.alt.storage.CaseStorage;
import com.frostsmp.alt.storage.PlayerStateStorage;
import com.frostsmp.alt.util.MessageUtil;
import org.bukkit.entity.Player;

public final class AppealService {

    private final PluginConfig config;
    private final CaseStorage caseStorage;
    private final PlayerStateStorage playerStateStorage;

    public AppealService(
            PluginConfig config,
            CaseStorage caseStorage,
            PlayerStateStorage playerStateStorage
    ) {
        this.config = config;
        this.caseStorage = caseStorage;
        this.playerStateStorage = playerStateStorage;
    }

    public boolean submitAppeal(Player player, String reason) {
        if (!player.hasPermission("frostsmp.alt.appeal")) {
            return false;
        }
        PlayerStateStorage.PlayerRecord record = playerStateStorage.get(player.getUniqueId());
        if (record == null || record.getStatus() != PlayerStatus.APPEAL_REQUIRED) {
            MessageUtil.send(player, "<red>You do not have an open alt case.</red>");
            return false;
        }
        AltCase altCase = caseStorage.findOpenCaseForPlayer(player.getUniqueId());
        if (altCase == null) {
            MessageUtil.send(player, "<red>No open case found for your account.</red>");
            return false;
        }
        if (altCase.getAppealText() != null && !altCase.getAppealText().isBlank()) {
            MessageUtil.send(player, "<red>You already submitted an appeal for this case.</red>");
            return false;
        }
        altCase.setAppealText(reason);
        altCase.setAppealSubmittedAt(System.currentTimeMillis());
        caseStorage.save();
        MessageUtil.send(player, config.getAppealSubmittedMessage());
        return true;
    }
}
