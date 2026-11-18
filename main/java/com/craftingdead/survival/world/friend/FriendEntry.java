package com.craftingdead.survival.world.friend;

import java.util.UUID;

public record FriendEntry(UUID uuid, String name, boolean online) {
}
