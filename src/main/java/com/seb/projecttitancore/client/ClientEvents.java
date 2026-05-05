package com.seb.projecttitancore.client;

import com.seb.projecttitancore.ProjectTitanCore;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@EventBusSubscriber(modid = ProjectTitanCore.MODID, value = Dist.CLIENT)
public class ClientEvents {
    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(
                ProjectTitanCore.TITAN_CORE_BLOCK_ENTITY.get(),
                TitanCoreRenderer::new
        );
    }
}
