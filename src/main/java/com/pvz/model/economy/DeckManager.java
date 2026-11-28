package com.pvz.model.economy;

import com.pvz.core.GameManager;
import com.pvz.model.plant.PlantFactory;
import com.pvz.model.plant.PlantSpec;
import com.pvz.model.plant.PlantType;
import javafx.scene.canvas.GraphicsContext;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Seed Bank manager that displays only the plants permitted in the current map's allowedPlants.
 * Positioned to the right of TV1's Sun counter (x=20..180) and above GameBoard.START_Y (y=100).
 */
public class DeckManager {
    public static final double DEFAULT_START_X = 200.0;
    public static final double DEFAULT_START_Y = 12.0;
    public static final double DEFAULT_CARD_WIDTH = 72.0;
    public static final double DEFAULT_CARD_HEIGHT = 76.0;
    public static final double DEFAULT_CARD_GAP = 8.0;

    private final List<CardSlot> slots = new ArrayList<>();
    private CardSlot selectedSlot;

    public DeckManager(List<PlantType> allowedPlants, PlantFactory plantFactory) {
        this(allowedPlants, plantFactory,
                DEFAULT_START_X, DEFAULT_START_Y,
                DEFAULT_CARD_WIDTH, DEFAULT_CARD_HEIGHT, DEFAULT_CARD_GAP);
    }

    public DeckManager(List<PlantType> allowedPlants, PlantFactory plantFactory,
                       double startX, double startY,
                       double cardWidth, double cardHeight, double gap) {
        Objects.requireNonNull(allowedPlants, "allowedPlants must not be null");
        Objects.requireNonNull(plantFactory, "plantFactory must not be null");

        // Preserve order and deduplicate allowed plant types
        Set<PlantType> uniqueAllowed = new LinkedHashSet<>();
        for (PlantType type : allowedPlants) {
            if (type != null) {
                uniqueAllowed.add(type);
            }
        }

        int index = 0;
        for (PlantType type : uniqueAllowed) {
            PlantSpec spec = plantFactory.getSpec(type);
            double cardX = startX + index * (cardWidth + gap);
            slots.add(new CardSlot(type, spec, cardX, startY, cardWidth, cardHeight));
            index++;
        }
    }

    public List<CardSlot> getSlots() {
        return Collections.unmodifiableList(slots);
    }

    public List<PlantType> getAllowedPlantTypes() {
        List<PlantType> types = new ArrayList<>(slots.size());
        for (CardSlot slot : slots) {
            types.add(slot.getPlantType());
        }
        return Collections.unmodifiableList(types);
    }

    public boolean hasPlant(PlantType type) {
        for (CardSlot slot : slots) {
            if (slot.getPlantType() == type) {
                return true;
            }
        }
        return false;
    }

    public CardSlot getSlot(PlantType type) {
        for (CardSlot slot : slots) {
            if (slot.getPlantType() == type) {
                return slot;
            }
        }
        return null;
    }

    public CardSlot getSelectedSlot() {
        return selectedSlot;
    }

    public PlantType getSelectedPlantType() {
        return selectedSlot != null ? selectedSlot.getPlantType() : null;
    }

    public void clearSelection() {
        if (selectedSlot != null) {
            selectedSlot.setSelected(false);
            selectedSlot = null;
        }
    }

    /**
     * Handles a mouse click on the HUD deck bar.
     * @return true if a card slot was clicked (even if currently unaffordable/on cooldown).
     */
    public boolean handleMouseClick(double mouseX, double mouseY, int currentSun) {
        for (CardSlot slot : slots) {
            if (slot.contains(mouseX, mouseY)) {
                if (slot == selectedSlot) {
                    clearSelection();
                } else if (slot.isSelectable(currentSun)) {
                    clearSelection();
                    selectedSlot = slot;
                    selectedSlot.setSelected(true);
                }
                return true;
            }
        }
        return false;
    }

    /**
     * Selects a card directly by PlantType if it exists in this deck and is selectable.
     */
    public boolean selectPlant(PlantType type, int currentSun) {
        CardSlot slot = getSlot(type);
        if (slot == null || !slot.isSelectable(currentSun)) {
            return false;
        }
        clearSelection();
        selectedSlot = slot;
        selectedSlot.setSelected(true);
        return true;
    }

    /**
     * Spends sun via GameManager and starts cooldown for the currently selected plant.
     * Call this right after verifying the target board cell can accept the plant.
     * @return the planted PlantType if successful, or null if no valid selection / insufficient sun.
     */
    public PlantType consumeSelectedPlant(GameManager gameManager) {
        Objects.requireNonNull(gameManager, "gameManager must not be null");
        if (selectedSlot == null || !selectedSlot.isReady()) {
            return null;
        }
        if (!gameManager.spendSun(selectedSlot.getCost())) {
            return null;
        }
        PlantType plantedType = selectedSlot.getPlantType();
        selectedSlot.triggerCooldown();
        selectedSlot = null;
        return plantedType;
    }

    public void update(double deltaTime) {
        for (CardSlot slot : slots) {
            slot.update(deltaTime);
        }
    }

    public void render(GraphicsContext gc, int currentSun) {
        for (CardSlot slot : slots) {
            slot.render(gc, currentSun);
        }
    }
}
