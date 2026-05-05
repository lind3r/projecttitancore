package com.seb.projecttitancore;

import com.seb.projecttitancore.screen.TitanCoreScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@Mod(value = ProjectTitanCore.MODID, dist = Dist.CLIENT)
public class ProjectTitanCoreClient {
    public ProjectTitanCoreClient(IEventBus modEventBus) {
        modEventBus.addListener(ProjectTitanCoreClient::registerScreens);
    }

    private static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(ProjectTitanCore.TITAN_CORE_MENU.get(), TitanCoreScreen::new);
    }
}