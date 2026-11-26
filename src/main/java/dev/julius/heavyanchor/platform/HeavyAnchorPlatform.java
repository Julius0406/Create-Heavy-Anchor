package dev.julius.heavyanchor.platform;

import dev.julius.heavyanchor.event.HeavyAnchorCommonEvents;
import dev.ryanhcode.sable.platform.SableEventPlatform;

public final class HeavyAnchorPlatform {
    private static boolean initialized;

    private HeavyAnchorPlatform() {
    }

    public static void init() {
        if (initialized) {
            return;
        }

        initialized = true;
        SableEventPlatform.INSTANCE.onSubLevelContainerReady(HeavyAnchorCommonEvents::onContainerReady);
        SableEventPlatform.INSTANCE.onPhysicsTick(HeavyAnchorCommonEvents::onPhysicsTick);
    }
}
