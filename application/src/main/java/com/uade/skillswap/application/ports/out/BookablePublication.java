package com.uade.skillswap.application.ports.out;

import com.uade.skillswap.domain.common.SkillCoins;
import com.uade.skillswap.domain.common.UserId;
import com.uade.skillswap.domain.publication.TimeSlot;

import java.util.List;
import java.util.Optional;

// Lo que contratación necesita saber de una publicación; lo completa el área de publicaciones.
public record BookablePublication(long id, UserId authorId, boolean published, SkillCoins price, List<TimeSlot> slots) {

    public BookablePublication {
        slots = List.copyOf(slots);
    }

    public Optional<TimeSlot> findSlot(long slotId) {
        return slots.stream().filter(slot -> slot.getId() != null && slot.getId() == slotId).findFirst();
    }
}
