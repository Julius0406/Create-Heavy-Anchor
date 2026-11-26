package dev.julius.heavyanchor.index;

import dev.julius.heavyanchor.CreateHeavyAnchor;
import dev.julius.heavyanchor.data.PendingTowEndpoint;
import foundry.veil.platform.registry.RegistrationProvider;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;

import java.util.function.UnaryOperator;

public final class HeavyAnchorDataComponents {
    private static final RegistrationProvider<DataComponentType<?>> REGISTRY =
            RegistrationProvider.get(Registries.DATA_COMPONENT_TYPE, CreateHeavyAnchor.MODID);

    public static final DataComponentType<PendingTowEndpoint> PENDING_TOW_ENDPOINT = register(
            "pending_tow_endpoint",
            builder -> builder.persistent(PendingTowEndpoint.CODEC).networkSynchronized(PendingTowEndpoint.STREAM_CODEC)
    );

    private HeavyAnchorDataComponents() {
    }

    private static <T> DataComponentType<T> register(final String name, final UnaryOperator<DataComponentType.Builder<T>> builder) {
        final DataComponentType<T> type = builder.apply(DataComponentType.builder()).build();
        REGISTRY.register(name, () -> type);
        return type;
    }

    public static void register() {
    }
}
